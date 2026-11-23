package com.example.web

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Lightweight embedded HTTP server so users can open FitVerso in their
 * mobile browser (http://127.0.0.1:8765) or PC browser on the same Wi-Fi network.
 */
class LocalWebServer(private val context: Context, val port: Int = 8765) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var isRunning: Boolean = false
        private set

    fun start(): Result<List<String>> = runCatching {
        if (isRunning) return@runCatching availableUrls()
        val socket = ServerSocket(port)
        serverSocket = socket
        isRunning = true

        serverJob = scope.launch {
            while (isActive && !socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                launch { handleClient(client) }
            }
        }
        availableUrls()
    }

    fun stop() {
        isRunning = false
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    fun availableUrls(): List<String> {
        val urls = mutableListOf("http://127.0.0.1:$port")
        runCatching {
            val ifaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            for (iface in ifaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress
                        if (!ip.isNullOrBlank()) urls += "http://$ip:$port"
                    }
                }
            }
        }
        return urls.distinct()
    }

    private fun handleClient(client: Socket) {
        client.use { s ->
            val reader = BufferedReader(InputStreamReader(s.getInputStream()))
            val reqLine = reader.readLine().orEmpty()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isBlank()) break
            }

            val path = reqLine.split(" ").getOrNull(1).orEmpty()
            val out = s.getOutputStream()

            if (path.startsWith("/health")) {
                val json = """{"status":"ok","app":"FitVerso"}""".toByteArray(Charsets.UTF_8)
                writeHttp(out, "200 OK", "application/json; charset=utf-8", json)
            } else if (path.startsWith("/download/project.zip")) {
                val zipBytes = runCatching { readProjectZipBytes() }.getOrNull()
                if (zipBytes != null) {
                    writeHttp(
                        out = out,
                        status = "200 OK",
                        contentType = "application/zip",
                        body = zipBytes,
                        extraHeaders = "Content-Disposition: attachment; filename=\"FitVerso-Projeto-Completo.zip\"\r\n"
                    )
                } else {
                    val msg = "Arquivo ZIP do projeto indisponivel".toByteArray(Charsets.UTF_8)
                    writeHttp(out, "404 Not Found", "text/plain; charset=utf-8", msg)
                }
            } else {
                val htmlBytes = readWebAssetBytes()
                writeHttp(out, "200 OK", "text/html; charset=utf-8", htmlBytes)
            }
        }
    }

    private fun writeHttp(
        out: java.io.OutputStream,
        status: String,
        contentType: String,
        body: ByteArray,
        extraHeaders: String = ""
    ) {
        val headers = (
            "HTTP/1.1 $status\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                extraHeaders +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
        out.write(headers)
        out.write(body)
        out.flush()
    }

    fun readWebAssetBytes(): ByteArray =
        context.assets.open("fitverso_web.html").use { it.readBytes() }

    fun readProjectZipBytes(): ByteArray =
        context.assets.open("fitverso_projeto_completo.zip").use { it.readBytes() }

    fun release() {
        stop()
        scope.cancel()
    }
}

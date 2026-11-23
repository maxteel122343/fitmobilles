package com.example.ml

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.domain.model.ModelVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed interface ModelDownloadState {
    data object Idle : ModelDownloadState
    data class Downloading(
        val filename: String,
        val stepIndex: Int,
        val totalSteps: Int,
        val bytesRead: Long,
        val totalBytes: Long
    ) : ModelDownloadState {
        val fraction: Float
            get() = if (totalBytes > 0L) (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }
    data class Completed(val filename: String) : ModelDownloadState
    data class Error(val message: String) : ModelDownloadState
}

data class CachedModelInfo(
    val filename: String,
    val file: File,
    val sizeBytes: Long
)

/**
 * Manages Hugging Face ONNX model downloads (momolesang/InstantHMR) and user-imported .onnx files.
 */
class ModelRepository(private val context: Context) {
    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val customDir = File(modelsDir, "custom").apply { mkdirs() }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var cancelRequested = false

    fun getBodyModelFile(): File = File(modelsDir, ModelVariant.BODY_ONNX)

    fun getNetworkModelFile(variant: ModelVariant, fitLevel: Int): File =
        File(modelsDir, variant.onnxFilename(fitLevel))

    fun isBodyModelReady(): Boolean {
        val f = getBodyModelFile()
        return f.exists() && f.length() > 1024L
    }

    fun isVariantReady(variant: ModelVariant, fitLevel: Int): Boolean {
        val net = getNetworkModelFile(variant, fitLevel)
        return isBodyModelReady() && net.exists() && net.length() > 1024L
    }

    fun resolveActiveModels(
        variant: ModelVariant,
        fitLevel: Int,
        customOnnxPath: String?
    ): Pair<File, File>? {
        val body = getBodyModelFile()
        if (!body.exists() || body.length() <= 1024L) return null
        val net = if (!customOnnxPath.isNullOrBlank()) {
            File(customOnnxPath)
        } else {
            getNetworkModelFile(variant, fitLevel)
        }
        return if (net.exists() && net.length() > 1024L) net to body else null
    }

    fun listCachedModels(): List<CachedModelInfo> {
        val all = mutableListOf<CachedModelInfo>()
        modelsDir.listFiles()?.forEach { f ->
            if (f.isFile && f.extension == "onnx" && f.length() > 1024L) {
                all.add(CachedModelInfo(f.name, f, f.length()))
            }
        }
        customDir.listFiles()?.forEach { f ->
            if (f.isFile && f.extension == "onnx" && f.length() > 1024L) {
                all.add(CachedModelInfo("custom/${f.name}", f, f.length()))
            }
        }
        return all.sortedBy { it.filename }
    }

    fun deleteModel(file: File): Boolean = file.delete()

    fun cancelDownload() {
        cancelRequested = true
    }

    suspend fun downloadVariantAndBody(variant: ModelVariant, fitLevel: Int): Result<Pair<File, File>> =
        withContext(Dispatchers.IO) {
            cancelRequested = false
            val filesToFetch = mutableListOf<String>()
            val netName = variant.onnxFilename(fitLevel)
            if (!getNetworkModelFile(variant, fitLevel).let { it.exists() && it.length() > 1024L }) {
                filesToFetch.add(netName)
            }
            if (!isBodyModelReady()) {
                filesToFetch.add(ModelVariant.BODY_ONNX)
            }
            if (filesToFetch.isEmpty()) {
                return@withContext Result.success(getNetworkModelFile(variant, fitLevel) to getBodyModelFile())
            }

            try {
                filesToFetch.forEachIndexed { idx, filename ->
                    if (cancelRequested) {
                        _downloadState.value = ModelDownloadState.Idle
                        return@withContext Result.failure(Exception("Download cancelled"))
                    }
                    downloadSingleFile(filename, idx + 1, filesToFetch.size)
                }
                _downloadState.value = ModelDownloadState.Completed(netName)
                Result.success(getNetworkModelFile(variant, fitLevel) to getBodyModelFile())
            } catch (e: Exception) {
                _downloadState.value = ModelDownloadState.Error(e.message ?: "Download failed")
                Result.failure(e)
            }
        }

    private fun downloadSingleFile(filename: String, step: Int, totalSteps: Int) {
        val url = "https://huggingface.co/${ModelVariant.HF_REPO_ID}/resolve/main/$filename"
        val target = File(modelsDir, filename)
        val temp = File(modelsDir, "$filename.part")
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} downloading $filename")
            }
            val body = response.body ?: throw IllegalStateException("Empty response body for $filename")
            val contentLength = body.contentLength()
            var totalRead = 0L
            val buf = ByteArray(64 * 1024)

            _downloadState.value = ModelDownloadState.Downloading(filename, step, totalSteps, 0L, contentLength)
            body.byteStream().use { input ->
                FileOutputStream(temp).use { output ->
                    while (true) {
                        if (cancelRequested) {
                            temp.delete()
                            throw InterruptedException("Download cancelled")
                        }
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        totalRead += read
                        _downloadState.value = ModelDownloadState.Downloading(
                            filename, step, totalSteps, totalRead, contentLength
                        )
                    }
                    output.flush()
                }
            }
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
        }
    }

    suspend fun importCustomOnnx(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = queryDisplayName(uri) ?: "custom_instanthmr.onnx"
            val sanitized = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val finalName = if (sanitized.endsWith(".onnx")) sanitized else "$sanitized.onnx"
            val dest = File(customDir, finalName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            } ?: error("Cannot open selected ONNX file")
            dest
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }
}

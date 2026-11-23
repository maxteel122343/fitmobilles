package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun CameraPreviewPane(
    useFrontCamera: Boolean,
    shouldAnalyze: () -> Boolean,
    onFrameBitmap: (Bitmap) -> Unit,
    onCameraError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val provider = runCatching { providerFuture.get() }.getOrElse {
                onCameraError("Câmera indisponível neste dispositivo")
                return@Runnable
            }
            val preferred = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            val fallback = if (useFrontCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            val selector = when {
                runCatching { provider.hasCamera(preferred) }.getOrDefault(false) -> preferred
                runCatching { provider.hasCamera(fallback) }.getOrDefault(false) -> fallback
                else -> {
                    onCameraError("Nenhuma câmera detectada — use Foto ou Vídeo")
                    return@Runnable
                }
            }

            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            val resSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(480, 360),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
                .build()

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resSelector)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(executor) { imageProxy ->
                try {
                    if (shouldAnalyze()) {
                        val raw = imageProxy.toBitmap()
                        val rot = imageProxy.imageInfo.rotationDegrees
                        val oriented = if (rot != 0) {
                            val m = Matrix().apply { postRotate(rot.toFloat()) }
                            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also {
                                if (it !== raw) raw.recycle()
                            }
                        } else {
                            raw
                        }
                        onFrameBitmap(oriented)
                    }
                } finally {
                    imageProxy.close()
                }
            }

            val bound = runCatching {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }
            if (bound.isFailure) {
                // Retry with minimal default resolutions for constrained devices/emulators
                val fallbackAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                fallbackAnalysis.setAnalyzer(executor) { imageProxy ->
                    try {
                        if (shouldAnalyze()) {
                            val raw = imageProxy.toBitmap()
                            val rot = imageProxy.imageInfo.rotationDegrees
                            val oriented = if (rot != 0) {
                                val m = Matrix().apply { postRotate(rot.toFloat()) }
                                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, m, true).also {
                                    if (it !== raw) raw.recycle()
                                }
                            } else {
                                raw
                            }
                            onFrameBitmap(oriented)
                        }
                    } finally {
                        imageProxy.close()
                    }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, fallbackAnalysis)
                }.onFailure { err ->
                    onCameraError("Erro ao iniciar câmera: ${err.message ?: "use Foto/Vídeo"}")
                }
            }
        }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

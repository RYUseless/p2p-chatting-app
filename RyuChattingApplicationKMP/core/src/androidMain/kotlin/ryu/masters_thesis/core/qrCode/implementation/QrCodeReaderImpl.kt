package ryu.masters_thesis.core.qrCode.implementation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import ryu.masters_thesis.core.qrCode.domain.QrCodeReader
import java.util.concurrent.Executors

class QrCodeReaderImpl(
    private val context:        Context,
    private val previewView:    PreviewView,
    private val lifecycleOwner: LifecycleOwner,
) : QrCodeReader {

    private var cameraProvider: ProcessCameraProvider? = null
    private val executor = Executors.newSingleThreadExecutor()

    @ExperimentalGetImage
    @OptIn(ExperimentalGetImage::class)
    override fun start(onResult: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted")
            return
        }

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { useCase ->
                    useCase.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees,
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (!barcodes.isNullOrEmpty()) {
                                        val raw  = barcodes.first().rawValue ?: ""
                                        val safe = sanitize(raw)
                                        Log.d(TAG, "QR scanned: $safe")
                                        if (safe.isNotEmpty()) {
                                            onResult(safe)
                                            cameraProvider?.unbindAll()
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Scan failed: ${e.message}", e)
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                Log.e(TAG, "CameraX binding failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun stop() {
        cameraProvider?.unbindAll()
        executor.shutdown()
    }

    private fun sanitize(input: String): String {
        val trimmed = input
            .filter { it >= ' ' && it != '\u007f' }
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()

        val lower = trimmed.lowercase()
        val dangerous = listOf(
            "javascript:", "data:text/html", "vbscript:",
            "intent:", "shell:", "cmd:", "file://",
        )
        if (dangerous.any { lower.startsWith(it) }) return ""
        return trimmed.take(4096)
    }

    companion object {
        private const val TAG = "QrCodeReaderImpl"
    }
}
package ryu.masters_thesis.ryus_chatting_application.logic.QRCode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// object → singleton
object QRCodeReader {

    private const val TAG = "QRCodeReader"

    /**
     * Spustí kameru a čte QR kódy.
     * yet tp be implemented -- potreba zmenit gui
     * @param context     Activity/Fragment context
     * @param previewView PreviewView, do kterého se kreslí kamera
     * @param onResult    callback s dekódovaným a **sanitizovaným** textem
     */
    @OptIn(ExperimentalGetImage::class)
    fun start(
        context: Context,
        previewView: PreviewView,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,  // ← NOVÝ PARAMETR
        onResult: (String) -> Unit
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val scanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                    .build()
            )
            val analysisExecutor = Executors.newSingleThreadExecutor()
            //pokud o zlepseni funkcionality qr code loaderu
            val analysisUseCase = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { useCase ->
                    useCase.setAnalyzer(analysisExecutor) { imageProxy: ImageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    Log.d(TAG, "Barcodes detected: ${barcodes.size}")
                                    if (!barcodes.isNullOrEmpty()) {
                                        val raw = barcodes.first().rawValue ?: ""
                                        Log.d(TAG, "Raw QR content: $raw")
                                        val safe = sanitize(raw)
                                        Log.d(TAG, "Sanitized: $safe")
                                        onResult(safe)
                                        cameraProvider.unbindAll()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "QR scan failed: ${e.message}", e)
                                }
                                .addOnCompleteListener { imageProxy.close() }

                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,  // ← místo "context as LifecycleOwner"
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysisUseCase
                )
            } catch (e: Exception) {
                Log.e(TAG, "CameraX binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }


    /**
     * Základní sanity check proti exekuci / injection – vrací jen „bezpečný“ text:
     * - odřízne non-printable znaky,
     * - zakáže \n, \r, nulák,
     * - nedovolí začátky typu `javascript:`, `intent:`, `data:text/html`, atd.
     */
    private fun sanitize(input: String): String {
        val trimmed = input
            .filter { it >= ' ' && it != '\u007f' }       // printable ASCII
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()

        val lower = trimmed.lowercase()

        val dangerousPrefixes = listOf(
            "javascript:",
            "data:text/html",
            "vbscript:",
            "intent:",
            "shell:",
            "cmd:",
            "file://"
        )

        if (dangerousPrefixes.any { lower.startsWith(it) }) {
            // Utilitka pro logovani/zobrateni erroru
            return ""
        }

        // případně můžeš limitovat délku, aby ti někdo neposlal 5MB payload
        return trimmed.take(4096)
    }
}
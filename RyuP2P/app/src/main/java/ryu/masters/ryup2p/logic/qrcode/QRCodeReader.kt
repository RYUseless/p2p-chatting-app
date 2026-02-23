package ryu.masters.ryup2p.logic.qrcode

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
        onResult: (String) -> Unit
    ) {
        // check for permissions, if none, yeet out of here
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Camera permission not granted")
            // permission dialog řeš v Activity/Fragmentu, ne tady
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()

            val scanner = BarcodeScanning.getClient(options)

            val analysisExecutor = Executors.newSingleThreadExecutor()

            val analysisUseCase = ImageAnalysis.Builder()
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
                                    if (!barcodes.isNullOrEmpty()) {
                                        val raw = barcodes.first().rawValue ?: ""
                                        val safe = sanitize(raw)
                                        onResult(safe)
                                        // po prvním úspěchu stopneme analyzér
                                        cameraProvider.unbindAll()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "QR scan failed", e)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
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
            // můžeš logovat / zobrazit error, ale nevrátíš payload
            return ""
        }

        // případně můžeš limitovat délku, aby ti někdo neposlal 5MB payload
        return trimmed.take(4096)
    }
}

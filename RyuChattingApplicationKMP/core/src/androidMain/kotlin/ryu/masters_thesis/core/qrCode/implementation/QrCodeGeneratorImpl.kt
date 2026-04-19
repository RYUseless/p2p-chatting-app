package ryu.masters_thesis.core.qrCode.implementation

import android.graphics.Bitmap
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import ryu.masters_thesis.core.qrCode.data.QrVariables
import ryu.masters_thesis.core.qrCode.domain.QrCodeGenerator
import java.io.ByteArrayOutputStream

class QrCodeGeneratorImpl : QrCodeGenerator {

    override fun generate(content: String, sizePx: Int): QrVariables? {
        return try {
            val encoder = BarcodeEncoder()
            val bitmap  = encoder.encodeBitmap(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bytes   = bitmapToBytes(bitmap)
            QrVariables(
                content    = content,
                sizePx     = sizePx,
                imageBytes = bytes,
            )
        } catch (e: WriterException) {
            Log.e("QrCodeGeneratorImpl", "generate failed: ${e.message}", e)
            null
        }
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
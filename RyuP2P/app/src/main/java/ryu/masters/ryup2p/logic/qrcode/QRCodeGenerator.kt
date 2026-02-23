package ryu.masters.ryup2p.logic.qrcode

import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

object QRCodeGenerator {

    fun generate(content: String, sizePx: Int): QRVariables? {
        return try {
            val encoder = BarcodeEncoder()
            val bitmap = encoder.encodeBitmap(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx
            )
            QRVariables(
                bitmap = bitmap,
                sizePx = sizePx,
                content = content
            )
        } catch (e: WriterException) {
            Log.e("QRCodeGenerator", "generateQR failed", e)
            null
        }
    }
}

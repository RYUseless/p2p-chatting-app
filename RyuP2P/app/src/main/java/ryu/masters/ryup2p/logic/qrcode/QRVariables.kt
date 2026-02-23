package ryu.masters.ryup2p.logic.qrcode

import android.graphics.Bitmap

//maybe migrate into QRCodeGenerator
data class QRVariables(
    val bitmap: Bitmap,
    val content: String,
    val sizePx: Int
)

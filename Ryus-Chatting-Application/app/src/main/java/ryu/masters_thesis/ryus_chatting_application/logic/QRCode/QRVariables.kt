package ryu.masters_thesis.ryus_chatting_application.logic.QRCode

import android.graphics.Bitmap

//maybe migrate into QRCodeGenerator
data class QRVariables(
    val bitmap: Bitmap,
    val content: String,
    val sizePx: Int
)
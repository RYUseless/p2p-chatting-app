package ryu.masters_thesis.core.qrCode.domain

import ryu.masters_thesis.core.qrCode.data.QrVariables

interface QrCodeGenerator {
    fun generate(content: String, sizePx: Int): QrVariables?
}
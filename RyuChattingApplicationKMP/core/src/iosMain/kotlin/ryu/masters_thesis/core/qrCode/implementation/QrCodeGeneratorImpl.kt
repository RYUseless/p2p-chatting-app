package ryu.masters_thesis.core.qrCode.implementation

import ryu.masters_thesis.core.qrCode.data.QrVariables
import ryu.masters_thesis.core.qrCode.domain.QrCodeGenerator

// TODO: implementovat přes CoreImage / CIFilter
class QrCodeGeneratorImpl : QrCodeGenerator {
    override fun generate(content: String, sizePx: Int): QrVariables? = TODO("iOS: CoreImage")
}
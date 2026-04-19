package ryu.masters_thesis.core.qrCode.implementation

import ryu.masters_thesis.core.qrCode.domain.QrCodeReader

// TODO: implementovat přes AVFoundation
class QrCodeReaderImpl : QrCodeReader {
    override fun start(onResult: (String) -> Unit) = TODO("iOS: AVFoundation")
    override fun stop() = Unit
}
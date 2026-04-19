package ryu.masters_thesis.core.qrCode.domain

interface QrCodeReader {
    fun start(onResult: (String) -> Unit)
    fun stop()
}
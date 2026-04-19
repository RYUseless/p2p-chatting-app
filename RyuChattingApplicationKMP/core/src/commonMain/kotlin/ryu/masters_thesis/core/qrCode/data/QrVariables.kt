package ryu.masters_thesis.core.qrCode.data

data class QrVariables(
    val content:    String,
    val sizePx:     Int,
    val imageBytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as QrVariables

        if (sizePx != other.sizePx) return false
        if (content != other.content) return false
        if (!imageBytes.contentEquals(other.imageBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sizePx
        result = 31 * result + content.hashCode()
        result = 31 * result + imageBytes.contentHashCode()
        return result
    }
}
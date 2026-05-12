package ryu.masters_thesis.core.cryptographyUtils.data

data class SchnorrProof(
    val rBase64: String,  // bod R jako x||y (64 bytes)
    val sBase64: String   // skalár s
)
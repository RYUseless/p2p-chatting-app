package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.data

data class MeshRoom(
    val roomId: String,
    val hostNodeAddress: String,
    val displayName: String,
    val cost: Int,
    val isPasswordProtected: Boolean,
)
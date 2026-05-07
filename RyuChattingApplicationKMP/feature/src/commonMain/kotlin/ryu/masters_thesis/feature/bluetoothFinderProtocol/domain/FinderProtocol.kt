package ryu.masters_thesis.feature.bluetoothFinderProtocol.domain

// Client strana BFP — ptá se, kdo hostuje místnost
interface FinderProtocol {
    suspend fun findHost(
        roomId:     String,
        knownPeers: List<String>,  // peerBluetoothAddress z metadat
    ): String?  // BT adresa hostitele, nebo null → stáváš se serverem
}
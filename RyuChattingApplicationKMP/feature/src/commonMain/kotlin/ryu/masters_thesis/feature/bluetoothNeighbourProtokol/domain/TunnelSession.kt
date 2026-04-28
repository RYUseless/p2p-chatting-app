package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TunnelSession {
    val incomingMessages: Flow<String>
    val isActive: StateFlow<Boolean>

    fun send(payload: String)
    fun close()
}
package ryu.masters_thesis.presentation.home.domain

import kotlinx.coroutines.flow.StateFlow

interface ScanCoordinator {
    val activeOwner: StateFlow<ScanOwner?>
    suspend fun acquire(owner: ScanOwner, onRevoked: suspend () -> Unit)
    fun release(owner: ScanOwner)
}
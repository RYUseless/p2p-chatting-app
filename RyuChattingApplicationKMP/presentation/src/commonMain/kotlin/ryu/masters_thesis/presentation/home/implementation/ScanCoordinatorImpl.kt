package ryu.masters_thesis.presentation.home.implementation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ryu.masters_thesis.presentation.home.domain.ScanCoordinator
import ryu.masters_thesis.presentation.home.domain.ScanOwner

class ScanCoordinatorImpl : ScanCoordinator {

    private val _activeOwner    = MutableStateFlow<ScanOwner?>(null)
    override val activeOwner: StateFlow<ScanOwner?> = _activeOwner.asStateFlow()

    private val mutex           = Mutex()
    private var revokeCallback: (suspend () -> Unit)? = null

    override suspend fun acquire(owner: ScanOwner, onRevoked: suspend () -> Unit) {
        mutex.withLock {
            if (_activeOwner.value != null && _activeOwner.value != owner) {
                revokeCallback?.invoke()
            }
            _activeOwner.value = owner
            revokeCallback     = onRevoked
        }
    }

    override fun release(owner: ScanOwner) {
        if (_activeOwner.value == owner) {
            _activeOwner.value = null
            revokeCallback     = null
        }
    }
}
package ryu.masters_thesis.feature.bluetooth.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice
import ryu.masters_thesis.feature.bluetooth.domain.ConnectionState

class BluetoothControllerClient(
    context: Context,
    cryptoFactory: (channelId: String) -> CryptoManager,
) : BluetoothControllerBase(context, cryptoFactory) {

    private var discoveryJob:      Job? = null
    private var receiverRegistered      = false

    private val receiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(BluetoothConstants.TAG_CLIENT, "Discovery started")
                    _isSearching.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(BluetoothConstants.TAG_CLIENT, "Discovery finished, candidates=${_scannedDevices.value.size}")
                    _isSearching.value = false
                }
                android.bluetooth.BluetoothDevice.ACTION_FOUND -> handleDiscoveredDevice(intent)
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun handleDiscoveredDevice(intent: Intent) {
        val device: android.bluetooth.BluetoothDevice = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(
                android.bluetooth.BluetoothDevice.EXTRA_DEVICE,
                android.bluetooth.BluetoothDevice::class.java,
            ) ?: return
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(android.bluetooth.BluetoothDevice.EXTRA_DEVICE) ?: return
        }

        val deviceName = intent.getStringExtra(android.bluetooth.BluetoothDevice.EXTRA_NAME)
            ?: try { device.name } catch (e: SecurityException) {
                Log.w(BluetoothConstants.TAG_CLIENT, "Cannot read device name: ${e.message}")
                return
            } ?: return

        Log.v(BluetoothConstants.TAG_CLIENT, "ACTION_FOUND: name=$deviceName address=${device.address}")

        val existing = _scannedDevices.value.find { it.address == device.address }
        if (existing != null) {
            Log.d(BluetoothConstants.TAG_CLIENT, "Updating known device: ${device.address} newName=$deviceName")
            _scannedDevices.value = _scannedDevices.value.map {
                if (it.address == device.address)
                    it.copy(name = deviceName, roomId = extractRoomId(deviceName))
                else it
            }
            return
        }

        if (deviceName.startsWith(BluetoothConstants.APP_IDENTIFIER)) {
            val candidate = BluetoothDevice(
                name    = deviceName,
                address = device.address,
                roomId  = extractRoomId(deviceName),
            )
            _scannedDevices.value += candidate
            Log.i(BluetoothConstants.TAG_CLIENT, "Candidate added: name=$deviceName roomId=${candidate.roomId}")
        } else {
            Log.v(BluetoothConstants.TAG_CLIENT, "Filtered out (not app peer): $deviceName")
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    override fun startClientMode() {
        Log.d(BluetoothConstants.TAG_CLIENT, "startClientMode")
        resetState()
        _isServer.value       = false
        _scannedDevices.value = emptyList()

        unregisterReceiver()
        loadBondedCandidates()
        adapter?.cancelDiscovery()
        registerReceiver()
        val started = adapter?.startDiscovery()
        Log.d(BluetoothConstants.TAG_CLIENT, "startDiscovery=$started adapterEnabled=${adapter?.isEnabled}")
        _isSearching.value = true

        discoveryJob?.cancel()
        discoveryJob = scope.launch {
            delay(BluetoothConstants.DISCOVERY_TIMEOUT_MS)
            Log.d(BluetoothConstants.TAG_CLIENT, "Discovery timeout reached")
            adapter?.cancelDiscovery()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectToDevice(device: BluetoothDevice) {
        Log.d(BluetoothConstants.TAG_CLIENT, "connectToDevice: address=${device.address} roomId=${device.roomId}")
        withContext(Dispatchers.IO) {
            sessionDevice = device
            resetConnectionState()
            withContext(Dispatchers.Main) {
                _currentRoomId.value   = device.roomId
                _connectionState.value = ConnectionState.CONNECTING
            }
            connectionManager?.closeConnection()
            connectionManager = buildConnectionManager()
            val androidDevice = adapter?.getRemoteDevice(device.address)
            if (androidDevice != null) {
                Log.d(BluetoothConstants.TAG_CLIENT, "Calling connectAsClient for ${device.address}")
                connectionManager!!.connectAsClient(androidDevice)
            } else {
                Log.e(BluetoothConstants.TAG_CLIENT, "getRemoteDevice returned null for ${device.address}")
            }
        }
    }


    override fun submitClientPassword(channelId: String, password: String) {
        Log.d(BluetoothConstants.TAG_CLIENT, "submitClientPassword: channelId=$channelId")
        if (password.isBlank()) {
            Log.w(BluetoothConstants.TAG_CLIENT, "blank password rejected")
            _passwordError.value = "Password cannot be empty"
            return
        }
        val keyData = pendingKeyData[channelId] ?: run {
            Log.e(BluetoothConstants.TAG_CLIENT, "No pending key data for: $channelId")
            return
        }
        sessionPassword = password.trim()
        scope.launch(Dispatchers.IO) {
            try {
                val crypto = cryptoFactory(channelId)
                val ok     = crypto.initializeAsClient(keyData, password.trim())
                Log.d(BluetoothConstants.TAG_CLIENT, "initializeAsClient result=$ok")
                withContext(Dispatchers.Main) {
                    if (ok) {
                        cryptoManagers[channelId] = crypto
                        pendingKeyData.remove(channelId)
                        _needsPassword.value = false
                        _passwordError.value = null
                        connectionManager?.sendMessage(
                            buildPacket(BluetoothConstants.MSG_HANDSHAKE, channelId, BluetoothConstants.HANDSHAKE_CLIENT_READY)
                        )
                        Log.d(BluetoothConstants.TAG_CLIENT, "HANDSHAKE_CLIENT_READY sent")
                    } else {
                        Log.w(BluetoothConstants.TAG_CLIENT, "Password verification failed")
                        _passwordError.value = "Incorrect password"
                    }
                }
            } catch (e: Exception) {
                Log.e(BluetoothConstants.TAG_CLIENT, "submitClientPassword error: ${e.message}", e)
                withContext(Dispatchers.Main) { _passwordError.value = "Incorrect password" }
            }
        }
    }

    override fun unregisterReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(receiver)
            receiverRegistered = false
            _isSearching.value = false
            discoveryJob?.cancel()
            Log.d(BluetoothConstants.TAG_CLIENT, "Receiver unregistered")
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_CLIENT, "unregisterReceiver error: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun cleanup() {
        Log.d(BluetoothConstants.TAG_CLIENT, "cleanup")
        unregisterReceiver()
        connectionManager?.closeConnection()
        scope.cancel()
    }

    override fun submitServerPassword(channelId: String, password: String) = Unit

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun loadBondedCandidates() {
        val bonded = adapter?.bondedDevices ?: return
        Log.d(BluetoothConstants.TAG_CLIENT, "loadBondedCandidates: total bonded=${bonded.size}")
        val candidates = bonded
            .onEach { Log.v(BluetoothConstants.TAG_CLIENT, "BONDED: name=${it.name} address=${it.address}") }
            .filter { it.name?.startsWith(BluetoothConstants.APP_IDENTIFIER) == true }
            .map { BluetoothDevice(name = it.name, address = it.address, roomId = extractRoomId(it.name ?: "")) }
            .distinctBy { it.address }
        Log.i(BluetoothConstants.TAG_CLIENT, "Bonded candidates pre-loaded: ${candidates.size}")
        if (candidates.isNotEmpty()) _scannedDevices.value = candidates
    }

    private fun registerReceiver() {
        if (receiverRegistered) {
            Log.d(BluetoothConstants.TAG_CLIENT, "registerReceiver: already registered")
            return
        }
        try {
            val filter = IntentFilter().apply {
                addAction(android.bluetooth.BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(receiver, filter)
            }
            receiverRegistered = true
            Log.d(BluetoothConstants.TAG_CLIENT, "Receiver registered")
        } catch (e: Exception) {
            Log.e(BluetoothConstants.TAG_CLIENT, "registerReceiver error: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun reconnect() {
        val device   = sessionDevice   ?: return
        val password = sessionPassword ?: return
        Log.d(BluetoothConstants.TAG_CLIENT, "reconnect → ${device.address} roomId=${device.roomId}")
        withContext(Dispatchers.Main) { _connectionState.value = ConnectionState.RECONNECTING }
        withContext(Dispatchers.IO) {
            resetConnectionState()
            withContext(Dispatchers.Main) { _currentRoomId.value = device.roomId }
            connectionManager?.closeConnection()
            connectionManager = buildConnectionManager()
            val androidDevice = adapter?.getRemoteDevice(device.address)
            if (androidDevice != null) {
                connectionManager!!.connectAsClient(androidDevice)
            } else {
                withContext(Dispatchers.Main) {
                    _connectionState.value = ConnectionState.FAILED
                    _connectionError.value = "Device not found: ${device.address}"
                }
            }
        }
    }
}
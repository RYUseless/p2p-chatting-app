package ryu.masters_thesis.feature.bluetooth.implementation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.cancel
import ryu.masters_thesis.core.cryptographyUtils.domain.CryptoManager
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothConstants
import ryu.masters_thesis.feature.bluetooth.domain.BluetoothDevice

class BluetoothControllerServer(
    context: Context,
    cryptoFactory: (channelId: String) -> CryptoManager,
) : BluetoothControllerBase(context, cryptoFactory) {

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE])
    override fun submitServerPassword(channelId: String, password: String) {
        Log.d(BluetoothConstants.TAG_SERVER, "submitServerPassword: channelId=$channelId")
        if (password.isBlank()) {
            Log.w(BluetoothConstants.TAG_SERVER, "blank password rejected")
            _passwordError.value = "Password cannot be empty"
            return
        }
        resetState()
        _currentRoomId.value = channelId
        _isServer.value      = true

        val crypto          = cryptoFactory(channelId)
        val keyExchangeData = crypto.initializeAsServer(password.trim())
        cryptoManagers[channelId] = crypto
        Log.d(BluetoothConstants.TAG_SERVER, "Crypto init ok, keyDataLen=${keyExchangeData.length}")

        connectionManager = buildConnectionManager(
            onConnectedExtra = {
                connectionManager?.sendMessage(
                    buildPacket(BluetoothConstants.MSG_KEY_EXCHANGE, channelId, keyExchangeData)
                )
                Log.d(BluetoothConstants.TAG_SERVER, "KEY_EXCHANGE sent for $channelId")
            }
        )

        val advertisedName = "${BluetoothConstants.APP_IDENTIFIER}_${channelId}"
        Log.i(BluetoothConstants.TAG_SERVER, "Starting server as: $advertisedName")
        connectionManager?.startServer(advertisedName)
        makeDiscoverable()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun cleanup() {
        val channelId = _currentRoomId.value
        Log.d(BluetoothConstants.TAG_SERVER, "cleanup: channelId=$channelId isConnected=${_isConnected.value}")
        if (channelId != null && _isConnected.value) {
            connectionManager?.sendMessage(
                buildPacket(BluetoothConstants.MSG_DISCONNECT, channelId, BluetoothConstants.DISCONNECT_SERVER_CLOSED)
            )
            Log.i(BluetoothConstants.TAG_SERVER, "DISCONNECT sent for $channelId")
        } else {
            Log.d(BluetoothConstants.TAG_SERVER, "cleanup: skipping DISCONNECT (not connected)")
        }
        connectionManager?.closeConnection()
        scope.cancel()
        Log.d(BluetoothConstants.TAG_SERVER, "cleanup done")
    }

    override fun startClientMode()                                         = Unit
    override suspend fun connectToDevice(device: BluetoothDevice)          = Unit
    override fun submitClientPassword(channelId: String, password: String) = Unit
    override fun unregisterReceiver()                                      = Unit

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun makeDiscoverable() {
        Log.d(BluetoothConstants.TAG_SERVER, "makeDiscoverable: 300s")
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { context.startActivity(it) }
    }
}
package ryu.masters_thesis.ryu_chatting_application_kmp

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
//ryuoviny:
//core
import ryu.masters_thesis.core.cryptographyUtils.implementation.AESCryptoManagerImpl
import ryu.masters_thesis.core.cryptographyUtils.implementation.AesKeyManagerImpl
//presentation
import ryu.masters_thesis.presentation.chatroom.domain.BluetoothControllerSingleton
import ryu.masters_thesis.presentation.navigation.ui.AppNavigation
//feature:
import ryu.masters_thesis.feature.bluetooth.implementation.BluetoothControllerServer
import ryu.masters_thesis.feature.bluetooth.implementation.BluetoothControllerClient
class MainActivity : ComponentActivity() {

    private var permissionsGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = result.values.all { it }
        initApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasAllRequiredPermissions()) {
            permissionsGranted = true
            initApp()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    private fun initApp() {
        val bluetoothAdapter =
            (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

        if (bluetoothAdapter == null || !permissionsGranted) {
            setContent { AppNavigation() }
            return
        }

        val keyManager = AesKeyManagerImpl(this)
        val cryptoFactory: (String) -> AESCryptoManagerImpl = { channelId ->
            AESCryptoManagerImpl(roomId = channelId, keyManager = keyManager)
        }

        BluetoothControllerSingleton.server = BluetoothControllerServer(
            context       = this,
            cryptoFactory = cryptoFactory,
        )
        BluetoothControllerSingleton.client = BluetoothControllerClient(
            context       = this,
            cryptoFactory = cryptoFactory,
        )

        setContent { AppNavigation() }
    }

    private fun hasAllRequiredPermissions(): Boolean =
        requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
            )
        }
}
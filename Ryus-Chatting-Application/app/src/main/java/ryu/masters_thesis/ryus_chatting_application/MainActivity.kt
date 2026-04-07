package ryu.masters_thesis.ryus_chatting_application

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import ryu.masters_thesis.ryus_chatting_application.config.AppSettings
import ryu.masters_thesis.ryus_chatting_application.config.AppTheme
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothCleanupService
import ryu.masters_thesis.ryus_chatting_application.logic.bluetooth.BluetoothController
import ryu.masters_thesis.ryus_chatting_application.ui.AppNavGraph
import ryu.masters_thesis.ryus_chatting_application.ui.theme.RyusChattingApplicationTheme

class MainActivity : ComponentActivity() {

    private val bluetoothController by lazy { BluetoothController(this) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // BluetoothCleanupService — zajistí cleanup při swipe away
        //val serviceIntent = Intent(this, BluetoothCleanupService::class.java)
        //startForegroundService(serviceIntent)

        setContent {
            var settings by remember { mutableStateOf(AppSettings()) }

            val darkTheme = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.DARK   -> true
                AppTheme.LIGHT  -> false
            }

            RyusChattingApplicationTheme(darkTheme = darkTheme) {
                AppNavGraph(
                    modifier            = Modifier.fillMaxSize(),
                    settings            = settings,
                    onSettingsChange    = { settings = it },
                    bluetoothController = bluetoothController
                )
            }
        }
    }
}

//migrate soon
data class ChatRoom(val name: String, val active: Boolean)
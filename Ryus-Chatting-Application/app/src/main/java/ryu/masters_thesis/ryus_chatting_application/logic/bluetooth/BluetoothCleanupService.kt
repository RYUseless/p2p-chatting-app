package ryu.masters_thesis.ryus_chatting_application.logic.bluetooth

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class BluetoothCleanupService : Service() {

    companion object {
        var originalDeviceName: String? = null
        var bluetoothAdapter: BluetoothAdapter? = null
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bt_channel",
                "Bluetooth",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "bt_channel")
            .setContentTitle("RyuP2P")
            .setContentText("Active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("MissingPermission")
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("BluetoothCleanupService", "App swiped away, restoring Bluetooth name")

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter

        val prefs = getSharedPreferences("bluetooth_state", MODE_PRIVATE)
        val savedName = prefs.getString("original_bt_name", null)

        if (savedName != null && bluetoothAdapter != null) {
            bluetoothAdapter.name = savedName
            Log.d("BluetoothCleanupService", "Restored name to: $savedName")

            prefs.edit { remove("original_bt_name") }
        } else {
            Log.w("BluetoothCleanupService", "Cannot restore name: savedName=$savedName, adapter=$bluetoothAdapter")
        }

        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
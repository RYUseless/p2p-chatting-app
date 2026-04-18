package ryu.masters_thesis.feature.bluetooth.implementation

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit

class BluetoothCleanupService : Service() {

    companion object {
        var originalDeviceName: String?       = null
        var bluetoothAdapter:   BluetoothAdapter? = null
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bt_channel", "Bluetooth", NotificationManager.IMPORTANCE_MIN,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        startForeground(
            1,
            NotificationCompat.Builder(this, "bt_channel")
                .setContentTitle("RyuP2P")
                .setContentText("Active")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build()
        )
    }

    @SuppressLint("MissingPermission")
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("BluetoothCleanupService", "App removed, restoring BT name")

        val adapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val prefs   = getSharedPreferences("bluetooth_state", MODE_PRIVATE)
        val saved   = originalDeviceName ?: prefs.getString("original_bt_name", null)

        if (saved != null && adapter != null) {
            try {
                adapter.name = saved
                Log.d("BluetoothCleanupService", "Restored: $saved")
            } catch (e: SecurityException) {
                Log.w("BluetoothCleanupService", "Cannot restore: ${e.message}")
            }
            prefs.edit { remove("original_bt_name") }
        }
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
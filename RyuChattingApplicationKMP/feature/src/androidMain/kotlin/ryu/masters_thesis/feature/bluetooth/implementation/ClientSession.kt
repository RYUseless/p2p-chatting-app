package ryu.masters_thesis.feature.bluetooth.implementation

import android.bluetooth.BluetoothSocket
import java.io.BufferedReader
import java.io.InputStreamReader

data class ClientSession(
    val mac:    String,
    val socket: BluetoothSocket,
) {
    val bufferedReader: BufferedReader =
        BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

    fun sendMessage(message: String) {
        socket.outputStream.write((message + "\n").toByteArray(Charsets.UTF_8))
        socket.outputStream.flush()
    }

    fun readLine(): String? = bufferedReader.readLine()

    fun close() {
        try { bufferedReader.close() } catch (_: Exception) {}
        try { socket.close() }         catch (_: Exception) {}
    }
}
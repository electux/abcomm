package com.abcomm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.UUID

/**
 * Abstraction for communication. 
 * Respects Interface Segregation and Dependency Inversion.
 */
interface CommunicationProvider {
    fun connect(device: Any)
    fun sendCommand(command: String)
    fun disconnect()
    fun isConnected(): Boolean
    fun setStatusListener(listener: (String) -> Unit)
}

class BluetoothService : CommunicationProvider {

    private var socket: BluetoothSocket? = null
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var onStatusChange: ((String) -> Unit)? = null

    override fun setStatusListener(listener: (String) -> Unit) {
        this.onStatusChange = listener
    }

    @SuppressLint("MissingPermission")
    override fun connect(device: Any) {
        if (device !is BluetoothDevice) return
        
        Thread {
            try {
                onStatusChange?.invoke("Connecting")
                // Reflection hack for better Linux compatibility
                socket = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 4) as BluetoothSocket
                socket?.connect()
                onStatusChange?.invoke("Connected to ${device.name}")
            } catch (e: Exception) {
                Log.e("BluetoothService", "Connection failed", e)
                onStatusChange?.invoke("Connection failed")
                disconnect()
            }
        }.start()
    }

    override fun sendCommand(command: String) {
        socket?.let {
            if (it.isConnected) {
                try {
                    it.outputStream.write(command.toByteArray())
                    Log.d("BluetoothService", "Sent: $command")
                } catch (e: IOException) {
                    Log.e("BluetoothService", "Error sending data", e)
                    onStatusChange?.invoke("Send failed")
                }
            } else {
                onStatusChange?.invoke("Disconnected")
            }
        } ?: onStatusChange?.invoke("Disconnected")
    }

    override fun disconnect() {
        try {
            socket?.close()
            socket = null
            onStatusChange?.invoke("Disconnected")
        } catch (e: IOException) {
            Log.e("BluetoothService", "Error closing socket", e)
        }
    }

    override fun isConnected(): Boolean = socket?.isConnected ?: false
}

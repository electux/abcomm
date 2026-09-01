package com.abcomm.communication

import android.annotation.SuppressLint
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.abcomm.protocol.FrameParser
import com.abcomm.protocol.MicrohilFrameParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Bluetooth RFCOMM implementation of CommunicationProvider managing device pairing,
 * socket lifecycle, frame streaming, and coroutine-based background I/O.
 */
class BluetoothService(
    private val frameParserFactory: () -> FrameParser = { MicrohilFrameParser() },
    private val uuid: UUID = UUID.fromString(DEFAULT_SPP_UUID_STRING)
) : CommunicationProvider {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectJob: Job? = null
    private var readerJob: Job? = null

    private var socket: BluetoothSocket? = null
    private var onStatusChange: ((ConnectionStatus) -> Unit)? = null
    private var onResponseReceived: ((String) -> Unit)? = null

    override fun setStatusListener(listener: (ConnectionStatus) -> Unit) {
        this.onStatusChange = listener
    }

    override fun setResponseListener(listener: (String) -> Unit) {
        this.onResponseReceived = listener
    }

    @SuppressLint("MissingPermission")
    override fun connect(target: ConnectionTarget) {
        if (target !is ConnectionTarget.Bluetooth) {
            Log.w(TAG, "Invalid target type passed to BluetoothService: $target")
            return
        }

        connectJob?.cancel()
        connectJob = serviceScope.launch {
            try {
                val targetName = target.device.name ?: target.device.address
                onStatusChange?.invoke(ConnectionStatus.Connecting(targetName))
                val device = target.device

                // Reflection fallback for Linux / Pico Bluetooth stack compatibility
                val newSocket = device.javaClass.getMethod(METHOD_CREATE_RFCOMM_SOCKET, Int::class.javaPrimitiveType)
                    .invoke(device, DEFAULT_RFCOMM_CHANNEL) as BluetoothSocket
                newSocket.connect()

                socket = newSocket
                onStatusChange?.invoke(ConnectionStatus.Connected(targetName))
                startReaderJob()
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                onStatusChange?.invoke(ConnectionStatus.Error(ERROR_CONNECTION_FAILED))
                disconnect()
            }
        }
    }

    private fun startReaderJob() {
        readerJob?.cancel()
        val frameParser = frameParserFactory()
        readerJob = serviceScope.launch {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive && isConnected()) {
                try {
                    val bytesRead = socket?.inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val rawChunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        Log.d(TAG, "Raw chunk: $rawChunk")
                        val frames = frameParser.process(rawChunk)
                        for (frame in frames) {
                            Log.d(TAG, "Frame received: $frame")
                            onResponseReceived?.invoke(frame)
                        }
                    } else if (bytesRead == -1) {
                        Log.d(TAG, "End of Bluetooth stream reached (remote disconnected)")
                        break
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "Reader loop stopped: ${e.message}")
                    break
                }
            }
            if (isActive) {
                disconnect()
            }
        }
    }

    override fun sendCommand(command: String) {
        serviceScope.launch {
            socket?.let {
                if (it.isConnected) {
                    try {
                        withContext(Dispatchers.IO) {
                            it.outputStream.write(command.toByteArray(Charsets.UTF_8))
                            it.outputStream.flush()
                        }
                        Log.d(TAG, "Sent: $command")
                    } catch (e: IOException) {
                        Log.e(TAG, "Error sending data", e)
                        onStatusChange?.invoke(ConnectionStatus.Error(ERROR_SEND_FAILED))
                    }
                } else {
                    onStatusChange?.invoke(ConnectionStatus.Disconnected)
                }
            } ?: onStatusChange?.invoke(ConnectionStatus.Disconnected)
        }
    }

    override fun disconnect() {
        serviceScope.launch {
            readerJob?.cancel()
            connectJob?.cancel()
            try {
                withContext(Dispatchers.IO) {
                    socket?.close()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket", e)
            } finally {
                socket = null
                onStatusChange?.invoke(ConnectionStatus.Disconnected)
            }
        }
    }

    override fun isConnected(): Boolean = socket?.isConnected ?: false

    companion object {
        private const val TAG = "BluetoothService"
        const val DEFAULT_SPP_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
        const val DEFAULT_RFCOMM_CHANNEL = 4
        const val BUFFER_SIZE = 1024
        private const val METHOD_CREATE_RFCOMM_SOCKET = "createRfcommSocket"

        const val ERROR_CONNECTION_FAILED = "Bluetooth connection failed"
        const val ERROR_SEND_FAILED = "Bluetooth send failed"
    }
}

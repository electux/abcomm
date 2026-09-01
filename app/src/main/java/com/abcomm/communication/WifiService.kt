package com.abcomm.communication

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
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * TCP Socket implementation of CommunicationProvider managing network socket lifecycle,
 * streaming frame parsing, and coroutine-based background I/O.
 */
class WifiService(
    private val frameParserFactory: () -> FrameParser = { MicrohilFrameParser() }
) : CommunicationProvider {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var connectJob: Job? = null
    private var readerJob: Job? = null

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var onStatusChange: ((ConnectionStatus) -> Unit)? = null
    private var onResponseReceived: ((String) -> Unit)? = null

    override fun setStatusListener(listener: (ConnectionStatus) -> Unit) {
        this.onStatusChange = listener
    }

    override fun setResponseListener(listener: (String) -> Unit) {
        this.onResponseReceived = listener
    }

    override fun connect(target: ConnectionTarget) {
        if (target !is ConnectionTarget.Wifi) {
            Log.w(TAG, "Invalid target type passed to WifiService: $target")
            return
        }

        val targetAddress = "${target.host}:${target.port}"
        connectJob?.cancel()
        connectJob = serviceScope.launch {
            try {
                onStatusChange?.invoke(ConnectionStatus.Connecting(targetAddress))

                val newSocket = Socket()
                withContext(Dispatchers.IO) {
                    newSocket.connect(InetSocketAddress(target.host, target.port), CONNECT_TIMEOUT_MS)
                }

                socket = newSocket
                inputStream = newSocket.getInputStream()
                outputStream = newSocket.getOutputStream()

                onStatusChange?.invoke(ConnectionStatus.Connected(targetAddress))
                startReaderJob()

            } catch (e: Exception) {
                Log.e(TAG, "TCP Connection failed", e)
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
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val rawChunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        Log.d(TAG, "Raw chunk: $rawChunk")
                        val frames = frameParser.process(rawChunk)
                        for (frame in frames) {
                            Log.d(TAG, "Frame received: $frame")
                            onResponseReceived?.invoke(frame)
                        }
                    } else if (bytesRead == -1) {
                        Log.d(TAG, "End of TCP stream reached (remote disconnected)")
                        break
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "Reader loop interrupted: ${e.message}")
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
            val out = outputStream
            if (isConnected() && out != null) {
                try {
                    withContext(Dispatchers.IO) {
                        out.write(command.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    Log.d(TAG, "Sent: $command")
                } catch (e: IOException) {
                    Log.e(TAG, "Error sending data", e)
                    onStatusChange?.invoke(ConnectionStatus.Error(ERROR_SEND_FAILED))
                }
            } else {
                onStatusChange?.invoke(ConnectionStatus.Disconnected)
            }
        }
    }

    override fun disconnect() {
        serviceScope.launch {
            readerJob?.cancel()
            connectJob?.cancel()
            try {
                withContext(Dispatchers.IO) {
                    inputStream?.close()
                    outputStream?.close()
                    socket?.close()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error closing socket", e)
            } finally {
                inputStream = null
                outputStream = null
                socket = null
                onStatusChange?.invoke(ConnectionStatus.Disconnected)
            }
        }
    }

    override fun isConnected(): Boolean {
        val s = socket
        return s != null && s.isConnected && !s.isClosed
    }

    companion object {
        private const val TAG = "WifiService"
        const val CONNECT_TIMEOUT_MS = 5000
        const val BUFFER_SIZE = 1024

        const val ERROR_CONNECTION_FAILED = "TCP connection failed"
        const val ERROR_SEND_FAILED = "TCP send failed"
    }
}

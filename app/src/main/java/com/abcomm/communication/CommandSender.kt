package com.abcomm.communication

/**
 * Dispatches raw command frames across an active communication channel.
 */
interface CommandSender {
    fun sendCommand(command: String)
}

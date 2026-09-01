package com.abcomm.communication

/**
 * Controls connection lifecycle (initiating, terminating, and checking connection state).
 */
interface ConnectionController {
    fun connect(target: ConnectionTarget)
    fun disconnect()
    fun isConnected(): Boolean
}

package com.abcomm.communication

/**
 * Type-safe model representing all possible states of a device communication connection.
 */
sealed interface ConnectionStatus {
    object Disconnected : ConnectionStatus
    data class Connecting(val target: String = "") : ConnectionStatus
    data class Connected(val targetName: String) : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

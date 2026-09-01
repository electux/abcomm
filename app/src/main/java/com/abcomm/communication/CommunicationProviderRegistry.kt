package com.abcomm.communication

/**
 * Registry resolving the appropriate CommunicationProvider instance based on the active ConnectionMode.
 */
interface CommunicationProviderRegistry {
    fun getProvider(mode: ConnectionMode): CommunicationProvider
}

package com.abcomm.communication

/**
 * Default implementation of CommunicationProviderRegistry.
 */
class DefaultCommunicationProviderRegistry(
    private val providers: Map<ConnectionMode, CommunicationProvider>
) : CommunicationProviderRegistry {

    override fun getProvider(mode: ConnectionMode): CommunicationProvider {
        return providers[mode] ?: throw IllegalArgumentException("Unsupported connection mode: $mode")
    }
}

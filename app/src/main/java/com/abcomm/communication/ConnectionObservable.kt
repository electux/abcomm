package com.abcomm.communication

/**
 * Provides subscription mechanisms for connection status changes and incoming frame responses.
 */
interface ConnectionObservable {
    fun setStatusListener(listener: (ConnectionStatus) -> Unit)
    fun setResponseListener(listener: (String) -> Unit)
}

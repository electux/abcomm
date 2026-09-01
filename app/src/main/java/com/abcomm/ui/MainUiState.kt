package com.abcomm.ui

import com.abcomm.communication.ConnectionMode
import com.abcomm.communication.ConnectionStatus
import com.abcomm.protocol.MicrohilProtocolConstants
import com.abcomm.settings.AppSettings

/**
 * Immutable state representation consumed by the UI layer, detailing connectivity,
 * relay channel boolean states, hardware IDs, and firmware info.
 */
data class MainUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val channelStates: List<Boolean> = List(MicrohilProtocolConstants.CHANNEL_COUNT) { false },
    val connectionMode: ConnectionMode = ConnectionMode.BLE,
    val wifiHost: String = AppSettings.DEFAULT_WIFI_IP,
    val wifiPort: Int = AppSettings.DEFAULT_WIFI_PORT,
    val lastResponse: String = "",
    val boardId: String = "",
    val firmwareVersion: String = ""
) {
    val isConnected: Boolean
        get() = connectionStatus is ConnectionStatus.Connected

    val isConnecting: Boolean
        get() = connectionStatus is ConnectionStatus.Connecting
}

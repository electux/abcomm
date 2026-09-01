package com.abcomm.settings

import com.abcomm.communication.ConnectionMode

/**
 * Domain model representing application configuration.
 */
data class AppSettings(
    val wifiIp: String = DEFAULT_WIFI_IP,
    val wifiPort: Int = DEFAULT_WIFI_PORT,
    val connectionMode: ConnectionMode = ConnectionMode.BLE
) {
    companion object {
        const val DEFAULT_WIFI_IP = "192.168.1.100"
        const val DEFAULT_WIFI_PORT = 5000
        const val MIN_PORT = 1
        const val MAX_PORT = 65535
    }
}

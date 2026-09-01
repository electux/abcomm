package com.abcomm.settings

import com.abcomm.communication.ConnectionMode

/**
 * Repository interface providing persistent storage for Wi-Fi settings and connection mode.
 */
interface AppSettingsRepository {
    fun getSettings(): AppSettings
    fun saveWifiTarget(ip: String, port: Int)
    fun saveConnectionMode(mode: ConnectionMode)
}

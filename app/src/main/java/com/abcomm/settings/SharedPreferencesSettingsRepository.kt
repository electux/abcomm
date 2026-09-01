package com.abcomm.settings

import android.content.Context
import android.content.SharedPreferences
import com.abcomm.communication.ConnectionMode

/**
 * SharedPreferences implementation of AppSettingsRepository.
 */
class SharedPreferencesSettingsRepository(
    private val sharedPreferences: SharedPreferences
) : AppSettingsRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    override fun getSettings(): AppSettings {
        val ip = sharedPreferences.getString(KEY_WIFI_IP, AppSettings.DEFAULT_WIFI_IP)
            ?: AppSettings.DEFAULT_WIFI_IP
        val port = sharedPreferences.getInt(KEY_WIFI_PORT, AppSettings.DEFAULT_WIFI_PORT)
        val modeStr = sharedPreferences.getString(KEY_CONN_MODE, ConnectionMode.BLE.name)
            ?: ConnectionMode.BLE.name
        val mode = try {
            ConnectionMode.valueOf(modeStr)
        } catch (_: Exception) {
            ConnectionMode.BLE
        }
        return AppSettings(wifiIp = ip, wifiPort = port, connectionMode = mode)
    }

    override fun saveWifiTarget(ip: String, port: Int) {
        sharedPreferences.edit()
            .putString(KEY_WIFI_IP, ip)
            .putInt(KEY_WIFI_PORT, port)
            .apply()
    }

    override fun saveConnectionMode(mode: ConnectionMode) {
        sharedPreferences.edit()
            .putString(KEY_CONN_MODE, mode.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "abcomm_prefs"
        private const val KEY_WIFI_IP = "wifi_ip"
        private const val KEY_WIFI_PORT = "wifi_port"
        private const val KEY_CONN_MODE = "conn_mode"
    }
}

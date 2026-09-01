package com.abcomm

import android.content.SharedPreferences
import com.abcomm.communication.ConnectionMode
import com.abcomm.settings.AppSettingsRepository
import com.abcomm.settings.SharedPreferencesSettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SharedPreferencesSettingsRepositoryTest {

    private val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private lateinit var repository: AppSettingsRepository

    @Before
    fun setUp() {
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        repository = SharedPreferencesSettingsRepository(sharedPreferences)
    }

    @Test
    fun `getSettings returns stored settings when present`() {
        every { sharedPreferences.getString("wifi_ip", any()) } returns "192.168.4.1"
        every { sharedPreferences.getInt("wifi_port", any()) } returns 8080
        every { sharedPreferences.getString("conn_mode", any()) } returns "WIFI"

        val settings = repository.getSettings()
        assertEquals("192.168.4.1", settings.wifiIp)
        assertEquals(8080, settings.wifiPort)
        assertEquals(ConnectionMode.WIFI, settings.connectionMode)
    }

    @Test
    fun `saveWifiTarget writes to shared preferences`() {
        repository.saveWifiTarget("10.0.0.1", 9000)

        verify { editor.putString("wifi_ip", "10.0.0.1") }
        verify { editor.putInt("wifi_port", 9000) }
        verify { editor.apply() }
    }

    @Test
    fun `saveConnectionMode writes to shared preferences`() {
        repository.saveConnectionMode(ConnectionMode.WIFI)

        verify { editor.putString("conn_mode", "WIFI") }
        verify { editor.apply() }
    }
}

package com.abcomm

import com.abcomm.settings.AppSettings
import com.abcomm.settings.AppSettingsRepository
import com.abcomm.ui.MainViewModel
import com.abcomm.ui.MainViewModelFactory
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class MainViewModelFactoryTest {

    @Test
    fun `create returns MainViewModel instance when requested`() {
        val settingsRepository = mockk<AppSettingsRepository>()
        every { settingsRepository.getSettings() } returns AppSettings()
        val factory = MainViewModelFactory(settingsRepository)

        val viewModel = factory.create(MainViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create throws IllegalArgumentException for unknown ViewModel class`() {
        val settingsRepository = mockk<AppSettingsRepository>()
        every { settingsRepository.getSettings() } returns AppSettings()
        val factory = MainViewModelFactory(settingsRepository)

        class UnknownViewModel : androidx.lifecycle.ViewModel()
        factory.create(UnknownViewModel::class.java)
    }
}

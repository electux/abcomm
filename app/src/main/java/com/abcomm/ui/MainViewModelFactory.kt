package com.abcomm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abcomm.communication.BluetoothService
import com.abcomm.communication.CommunicationProvider
import com.abcomm.communication.ConnectionMode
import com.abcomm.communication.DefaultCommunicationProviderRegistry
import com.abcomm.communication.WifiService
import com.abcomm.protocol.CommandFormatter
import com.abcomm.protocol.MicrohilCommandFormatter
import com.abcomm.protocol.MicrohilResponseParser
import com.abcomm.protocol.ResponseParser
import com.abcomm.settings.AppSettingsRepository

/**
 * ViewModelProvider.Factory assembling dependencies and creating MainViewModel instances.
 */
class MainViewModelFactory(
    private val settingsRepository: AppSettingsRepository,
    private val bluetoothService: CommunicationProvider = BluetoothService(),
    private val wifiService: CommunicationProvider = WifiService(),
    private val commandFormatter: CommandFormatter = MicrohilCommandFormatter(),
    private val responseParser: ResponseParser = MicrohilResponseParser()
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val registry = DefaultCommunicationProviderRegistry(
                mapOf(
                    ConnectionMode.BLE to bluetoothService,
                    ConnectionMode.WIFI to wifiService
                )
            )
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                providerRegistry = registry,
                commandFormatter = commandFormatter,
                responseParser = responseParser,
                settingsRepository = settingsRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

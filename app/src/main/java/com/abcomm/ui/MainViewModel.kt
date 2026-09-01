package com.abcomm.ui

import androidx.lifecycle.ViewModel
import com.abcomm.communication.CommunicationProvider
import com.abcomm.communication.CommunicationProviderRegistry
import com.abcomm.communication.ConnectionMode
import com.abcomm.communication.ConnectionStatus
import com.abcomm.communication.ConnectionTarget
import com.abcomm.communication.DefaultCommunicationProviderRegistry
import com.abcomm.protocol.CommandFormatter
import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.MicrohilCommandFormatter
import com.abcomm.protocol.MicrohilProtocolConstants
import com.abcomm.protocol.MicrohilResponseParser
import com.abcomm.protocol.ResponseParser
import com.abcomm.settings.AppSettings
import com.abcomm.settings.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages UI state, coordinates communication providers across Bluetooth/Wi-Fi modes,
 * dispatches microHIL formatted commands, and processes incoming device telemetry.
 */
class MainViewModel(
    val providerRegistry: CommunicationProviderRegistry,
    val commandFormatter: CommandFormatter = MicrohilCommandFormatter(),
    val responseParser: ResponseParser = MicrohilResponseParser(),
    val settingsRepository: AppSettingsRepository? = null
) : ViewModel() {

    // Test convenience constructor for single mock communicator
    constructor(communicator: CommunicationProvider) : this(
        providerRegistry = DefaultCommunicationProviderRegistry(
            mapOf(
                ConnectionMode.BLE to communicator,
                ConnectionMode.WIFI to communicator
            )
        )
    )

    private val _uiState: MutableStateFlow<MainUiState> = run {
        val initialSettings = settingsRepository?.getSettings()
        MutableStateFlow(
            MainUiState(
                connectionMode = initialSettings?.connectionMode ?: ConnectionMode.BLE,
                wifiHost = initialSettings?.wifiIp ?: AppSettings.DEFAULT_WIFI_IP,
                wifiPort = initialSettings?.wifiPort ?: AppSettings.DEFAULT_WIFI_PORT
            )
        )
    }
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var activeCommunicator: CommunicationProvider

    init {
        activeCommunicator = providerRegistry.getProvider(_uiState.value.connectionMode)
        setupActiveCommunicator(activeCommunicator)
    }

    private fun setupActiveCommunicator(provider: CommunicationProvider) {
        provider.setStatusListener { status ->
            if (provider === activeCommunicator) {
                updateStatus(status)
            }
        }
        provider.setResponseListener { response ->
            if (provider === activeCommunicator) {
                handleIncomingResponse(response)
            }
        }
    }

    private fun updateStatus(status: ConnectionStatus) {
        val wasConnected = _uiState.value.isConnected
        val isNowConnected = status is ConnectionStatus.Connected

        _uiState.value = _uiState.value.copy(
            connectionStatus = status,
            channelStates = if (isNowConnected) _uiState.value.channelStates else List(MicrohilProtocolConstants.CHANNEL_COUNT) { false },
            boardId = if (isNowConnected) _uiState.value.boardId else "",
            firmwareVersion = if (isNowConnected) _uiState.value.firmwareVersion else ""
        )

        // Automatically query status and device info when connection is established
        if (isNowConnected && !wasConnected) {
            queryAllStatus()
            queryDeviceInfo()
        }
    }

    private fun handleIncomingResponse(rawResponse: String) {
        _uiState.value = _uiState.value.copy(lastResponse = rawResponse)

        when (val parsed = responseParser.parse(rawResponse)) {
            is DeviceResponse.ChannelState -> {
                if (parsed.channel in MicrohilProtocolConstants.MIN_CHANNEL..MicrohilProtocolConstants.MAX_CHANNEL) {
                    val newStates = _uiState.value.channelStates.toMutableList()
                    newStates[parsed.channel - 1] = parsed.isOn
                    _uiState.value = _uiState.value.copy(channelStates = newStates)
                }
            }
            is DeviceResponse.AllChannelsState -> {
                _uiState.value = _uiState.value.copy(
                    channelStates = List(MicrohilProtocolConstants.CHANNEL_COUNT) { parsed.isOn }
                )
            }
            is DeviceResponse.AllChannelsSnapshot -> {
                if (parsed.states.size == MicrohilProtocolConstants.CHANNEL_COUNT) {
                    _uiState.value = _uiState.value.copy(channelStates = parsed.states)
                }
            }
            is DeviceResponse.BoardId -> {
                _uiState.value = _uiState.value.copy(boardId = parsed.id)
            }
            is DeviceResponse.FirmwareVersion -> {
                _uiState.value = _uiState.value.copy(firmwareVersion = parsed.version)
            }
            is DeviceResponse.SystemResetting -> {
                _uiState.value = _uiState.value.copy(
                    channelStates = List(MicrohilProtocolConstants.CHANNEL_COUNT) { false },
                    boardId = "",
                    firmwareVersion = ""
                )
            }
            is DeviceResponse.Unknown -> {
                // Keep raw response stored
            }
        }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        if (_uiState.value.connectionMode == mode) return
        if (isConnected()) {
            disconnect()
        }
        activeCommunicator = providerRegistry.getProvider(mode)
        setupActiveCommunicator(activeCommunicator)

        _uiState.value = _uiState.value.copy(
            connectionMode = mode,
            connectionStatus = ConnectionStatus.Disconnected,
            boardId = "",
            firmwareVersion = ""
        )
        settingsRepository?.saveConnectionMode(mode)
    }

    fun setWifiTarget(host: String, port: Int) {
        _uiState.value = _uiState.value.copy(
            wifiHost = host,
            wifiPort = port
        )
        settingsRepository?.saveWifiTarget(host, port)
    }

    fun connect(target: ConnectionTarget) {
        activeCommunicator.connect(target)
    }

    fun disconnect() {
        activeCommunicator.disconnect()
    }

    fun toggleChannel(index: Int) {
        if (!_uiState.value.isConnected) return
        require(index in 0 until MicrohilProtocolConstants.CHANNEL_COUNT) {
            "Channel index must be 0 until ${MicrohilProtocolConstants.CHANNEL_COUNT}"
        }

        val currentState = _uiState.value.channelStates[index]
        val newState = !currentState

        val newStates = _uiState.value.channelStates.toMutableList()
        newStates[index] = newState
        _uiState.value = _uiState.value.copy(channelStates = newStates)

        val channelNum = index + 1
        activeCommunicator.sendCommand(commandFormatter.formatChannel(channelNum, newState))
    }

    fun setAllChannels(on: Boolean) {
        if (!_uiState.value.isConnected) return

        activeCommunicator.sendCommand(commandFormatter.formatAllChannels(on))
        _uiState.value = _uiState.value.copy(
            channelStates = List(MicrohilProtocolConstants.CHANNEL_COUNT) { on }
        )
    }

    fun queryAllStatus() {
        if (_uiState.value.isConnected) {
            activeCommunicator.sendCommand(commandFormatter.formatQueryAllStatus())
        }
    }

    fun queryDeviceInfo() {
        if (_uiState.value.isConnected) {
            activeCommunicator.sendCommand(commandFormatter.formatQueryBoardId())
            activeCommunicator.sendCommand(commandFormatter.formatQueryVersion())
        }
    }

    fun resetDevice() {
        if (_uiState.value.isConnected) {
            activeCommunicator.sendCommand(commandFormatter.formatReset())
        }
    }

    fun isConnected(): Boolean = activeCommunicator.isConnected()
}

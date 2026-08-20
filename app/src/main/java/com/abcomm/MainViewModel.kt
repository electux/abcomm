package com.abcomm

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI State for the Main Screen.
 * Respects SRP (Single Responsibility Principle) by separating State from Logic.
 */
data class MainUiState(
    val status: String = "Disconnected",
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val channelStates: List<Boolean> = List(8) { false }
)

class MainViewModel(private val communicator: CommunicationProvider) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        communicator.setStatusListener { status ->
            updateStatus(status)
        }
    }

    private fun updateStatus(status: String) {
        val isConnected = status.startsWith("Connected")
        val isConnecting = status.startsWith("Connecting")
        _uiState.value = _uiState.value.copy(
            status = status,
            isConnected = isConnected,
            isConnecting = isConnecting
        )
    }

    fun connect(device: Any) {
        communicator.connect(device)
    }

    fun disconnect() {
        communicator.disconnect()
    }

    fun toggleChannel(index: Int) {
        if (!_uiState.value.isConnected) return

        val currentState = _uiState.value.channelStates[index]
        val newState = !currentState
        
        val newStates = _uiState.value.channelStates.toMutableList()
        newStates[index] = newState
        _uiState.value = _uiState.value.copy(channelStates = newStates)

        val channelNum = index + 1
        val cmdState = if (newState) "on" else "off"
        communicator.sendCommand("mh#ch#${channelNum}#${cmdState}#end")
    }

    fun setAllChannels(on: Boolean) {
        if (!_uiState.value.isConnected) return

        val stateStr = if (on) "on" else "off"
        communicator.sendCommand("mh#ch#all#$stateStr#end")
        
        _uiState.value = _uiState.value.copy(
            channelStates = List(8) { on }
        )
    }
    
    fun isConnected() = communicator.isConnected()
}

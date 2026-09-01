package com.abcomm.protocol

/**
 * Domain model representing typed responses received from microHIL firmware.
 */
sealed interface DeviceResponse {
    data class ChannelState(val channel: Int, val isOn: Boolean) : DeviceResponse
    data class AllChannelsState(val isOn: Boolean) : DeviceResponse
    data class AllChannelsSnapshot(val states: List<Boolean>) : DeviceResponse
    data class BoardId(val id: String) : DeviceResponse
    data class FirmwareVersion(val version: String) : DeviceResponse
    object SystemResetting : DeviceResponse
    data class Unknown(val raw: String) : DeviceResponse
}

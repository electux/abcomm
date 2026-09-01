package com.abcomm.communication

import android.bluetooth.BluetoothDevice

/**
 * Defines destination endpoints for device connectivity (Bluetooth RFCOMM or TCP Wi-Fi).
 */
sealed interface ConnectionTarget {
    data class Bluetooth(val device: BluetoothDevice) : ConnectionTarget
    data class Wifi(val host: String, val port: Int) : ConnectionTarget
}

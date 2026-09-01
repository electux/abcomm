package com.abcomm.ui

import android.bluetooth.BluetoothDevice

/**
 * Interface defining contract for querying Bluetooth availability and bonded devices.
 */
interface BluetoothDeviceProvider {
    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun getBondedDevices(): List<BluetoothDevice>
}

package com.abcomm.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.content.ContextCompat

/**
 * Wraps Android BluetoothManager and adapter to inspect Bluetooth state and retrieve paired/bonded devices.
 */
class BluetoothDeviceManager(private val context: Context) : BluetoothDeviceProvider {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
        manager?.adapter
    }

    override fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override fun getBondedDevices(): List<BluetoothDevice> {
        val paired = bluetoothAdapter?.bondedDevices
        return paired?.toList().orEmpty()
    }
}

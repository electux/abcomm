package com.abcomm.ui

import android.content.Context

/**
 * Interface defining contract for Bluetooth runtime permission checks.
 */
interface BluetoothPermissionChecker {
    fun getRequiredPermissions(): Array<String>
    fun getMissingPermissions(context: Context): List<String>
    fun hasAllPermissions(context: Context): Boolean
}

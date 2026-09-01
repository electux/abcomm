package com.abcomm

import com.abcomm.ui.BluetoothPermissionHelper
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothPermissionHelperTest {

    @Test
    fun `getRequiredPermissions returns non-empty permissions array`() {
        val helper = BluetoothPermissionHelper()
        val permissions = helper.getRequiredPermissions()
        assertNotNull(permissions)
        assertTrue(permissions.isNotEmpty())
    }
}

package com.abcomm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abcomm.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Simple Dependency Injection without Hilt for now
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(BluetoothService()) as T
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            showDeviceSelectionDialog()
        } else {
            Toast.makeText(this, getString(R.string.error_no_bluetooth), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnConnect.setOnClickListener {
            if (viewModel.isConnected()) {
                viewModel.disconnect()
            } else {
                checkPermissionsAndConnect()
            }
        }

        val buttons = listOf(
            binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8
        )
        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewModel.toggleChannel(index)
            }
        }

        binding.btnAllOn.setOnClickListener { viewModel.setAllChannels(true) }
        binding.idAllOff.setOnClickListener { viewModel.setAllChannels(false) }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateStatusUI(state.status, state.isConnected, state.isConnecting)
                    updateChannelButtons(state.channelStates, state.isConnected)
                }
            }
        }
    }

    private fun updateStatusUI(status: String, isConnected: Boolean, isConnecting: Boolean) {
        val formattedStatus = if (status.startsWith("Connected to", ignoreCase = true)) {
            val deviceName = status.substringAfter("Connected to ").uppercase()
            getString(R.string.status_label, "\n" + getString(R.string.status_connected, deviceName))
        } else if (isConnecting) {
            getString(R.string.status_label, getString(R.string.status_initializing))
        } else {
            getString(R.string.status_label, getString(R.string.status_disconnected))
        }
        
        binding.tvStatus.text = formattedStatus
        
        when {
            isConnected -> {
                binding.btnConnect.text = getString(R.string.btn_disconnect)
                binding.btnConnect.setTextColor(ContextCompat.getColor(this, R.color.cyber_red))
                binding.btnConnect.setStrokeColorResource(R.color.cyber_red)
                binding.btnConnect.isEnabled = true
            }
            isConnecting -> {
                binding.btnConnect.text = getString(R.string.status_initializing)
                binding.btnConnect.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                binding.btnConnect.setStrokeColorResource(android.R.color.darker_gray)
                binding.btnConnect.isEnabled = false
            }
            else -> {
                binding.btnConnect.text = getString(R.string.btn_connect)
                binding.btnConnect.setTextColor(ContextCompat.getColor(this, R.color.cyber_cyan))
                binding.btnConnect.setStrokeColorResource(R.color.cyber_cyan)
                binding.btnConnect.isEnabled = true
            }
        }
    }

    private fun updateChannelButtons(states: List<Boolean>, isEnabled: Boolean) {
        val buttons = listOf(
            binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8
        )
        buttons.forEachIndexed { index, button ->
            button.isEnabled = isEnabled
            updateButtonStyle(button, index + 1, states[index])
        }
        binding.btnAllOn.isEnabled = isEnabled
        binding.idAllOff.isEnabled = isEnabled
    }

    private fun updateButtonStyle(button: MaterialButton, channelNum: Int, isChecked: Boolean) {
        val color = if (isChecked) {
            ContextCompat.getColor(this, R.color.cyber_green)
        } else {
            ContextCompat.getColor(this, android.R.color.transparent)
        }
        button.backgroundTintList = ColorStateList.valueOf(color)
        
        if (isChecked) {
            button.setTextColor(ContextCompat.getColor(this, R.color.black))
            button.setStrokeColorResource(R.color.cyber_green)
            button.text = getString(R.string.channel_active, channelNum)
        } else {
            button.setTextColor(ContextCompat.getColor(this, R.color.white))
            button.setStrokeColorResource(R.color.cyber_cyan)
            button.text = getString(R.string.channel_off, channelNum)
        }
    }

    private fun checkPermissionsAndConnect() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            showDeviceSelectionDialog()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceSelectionDialog() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.error_no_bluetooth), Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, getString(R.string.error_bt_disabled), Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceList = pairedDevices?.toList() ?: emptyList()

        if (deviceList.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_targets), Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = deviceList.map { it.name ?: getString(R.string.error_no_targets) }.toTypedArray()

        AlertDialog.Builder(this, R.style.CyberDialogTheme)
            .setTitle(getString(R.string.dialog_select_target))
            .setItems(deviceNames) { _, which ->
                viewModel.connect(deviceList[which])
            }
            .setNegativeButton(getString(R.string.dialog_abort), null)
            .show()
    }
}

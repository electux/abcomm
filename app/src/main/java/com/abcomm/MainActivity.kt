package com.abcomm

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abcomm.communication.ConnectionMode
import com.abcomm.communication.ConnectionStatus
import com.abcomm.communication.ConnectionTarget
import com.abcomm.databinding.ActivityMainBinding
import com.abcomm.settings.AppSettings
import com.abcomm.settings.AppSettingsRepository
import com.abcomm.settings.SharedPreferencesSettingsRepository
import com.abcomm.ui.BluetoothDeviceManager
import com.abcomm.ui.BluetoothDeviceProvider
import com.abcomm.ui.BluetoothPermissionChecker
import com.abcomm.ui.BluetoothPermissionHelper
import com.abcomm.ui.MainViewModel
import com.abcomm.ui.MainViewModelFactory
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Primary activity managing presentation, user interactions, and visual telemetry feedback.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val permissionHelper: BluetoothPermissionChecker = BluetoothPermissionHelper()
    private val deviceManager: BluetoothDeviceProvider by lazy { BluetoothDeviceManager(applicationContext) }

    private val settingsRepository: AppSettingsRepository by lazy {
        SharedPreferencesSettingsRepository(applicationContext)
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(settingsRepository)
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

        loadSavedPreferences()
        setupClickListeners()
        observeUiState()
    }

    private fun loadSavedPreferences() {
        val settings = settingsRepository.getSettings()
        binding.etWifiIp.setText(settings.wifiIp)
        binding.etWifiPort.setText(settings.wifiPort.toString())
    }

    private fun setupClickListeners() {
        binding.toggleModeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val selectedMode = when (checkedId) {
                    R.id.btn_mode_wifi -> ConnectionMode.WIFI
                    else -> ConnectionMode.BLE
                }
                viewModel.setConnectionMode(selectedMode)
            }
        }

        binding.btnConnect.setOnClickListener {
            if (viewModel.isConnected()) {
                viewModel.disconnect()
            } else {
                handleConnectAction()
            }
        }

        binding.btnSync.setOnClickListener {
            viewModel.queryAllStatus()
            viewModel.queryDeviceInfo()
            Toast.makeText(this, getString(R.string.toast_syncing), Toast.LENGTH_SHORT).show()
        }

        binding.btnReset.setOnClickListener {
            AlertDialog.Builder(this, R.style.CyberDialogTheme)
                .setTitle(getString(R.string.dialog_reboot_title))
                .setMessage(getString(R.string.dialog_reboot_message))
                .setPositiveButton(getString(R.string.dialog_reboot_confirm)) { _, _ ->
                    viewModel.resetDevice()
                }
                .setNegativeButton(getString(R.string.dialog_abort), null)
                .show()
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

    private fun handleConnectAction() {
        when (viewModel.uiState.value.connectionMode) {
            ConnectionMode.BLE -> checkPermissionsAndConnectBle()
            ConnectionMode.WIFI -> connectWifi()
        }
    }

    private fun connectWifi() {
        val ip = binding.etWifiIp.text?.toString()?.trim().orEmpty()
        val portStr = binding.etWifiPort.text?.toString()?.trim().orEmpty()
        val port = portStr.toIntOrNull()

        if (ip.isEmpty() || port == null || port !in AppSettings.MIN_PORT..AppSettings.MAX_PORT) {
            Toast.makeText(this, getString(R.string.error_invalid_wifi), Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.setWifiTarget(ip, port)
        viewModel.connect(ConnectionTarget.Wifi(host = ip, port = port))
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateModeUI(state.connectionMode)
                    updateStatusUI(state.connectionStatus)
                    updateDeviceInfoUI(state.boardId, state.firmwareVersion, state.isConnected)
                    updateChannelButtons(state.channelStates, state.isConnected)
                    updateResponseUI(state.lastResponse)
                }
            }
        }
    }

    private fun updateModeUI(mode: ConnectionMode) {
        val isWifi = mode == ConnectionMode.WIFI
        binding.layoutWifiSettings.visibility = if (isWifi) View.VISIBLE else View.GONE
        binding.toggleModeGroup.check(if (isWifi) R.id.btn_mode_wifi else R.id.btn_mode_ble)
    }

    private fun updateDeviceInfoUI(boardId: String, firmwareVersion: String, isConnected: Boolean) {
        if (isConnected && (boardId.isNotEmpty() || firmwareVersion.isNotEmpty())) {
            binding.tvDeviceInfo.visibility = View.VISIBLE
            val idText = if (boardId.isNotEmpty()) boardId else getString(R.string.label_not_available)
            val fwText = if (firmwareVersion.isNotEmpty()) firmwareVersion else getString(R.string.label_not_available)
            binding.tvDeviceInfo.text = getString(R.string.device_info_label, idText, fwText)
        } else {
            binding.tvDeviceInfo.visibility = View.GONE
        }
        binding.layoutQuickActions.visibility = if (isConnected) View.VISIBLE else View.GONE
    }

    private fun updateResponseUI(lastResponse: String) {
        if (lastResponse.isNotEmpty()) {
            binding.tvLastResponse.visibility = View.VISIBLE
            binding.tvLastResponse.text = getString(R.string.last_response_label, lastResponse)
        } else {
            binding.tvLastResponse.visibility = View.GONE
        }
    }

    private fun updateStatusUI(status: ConnectionStatus) {
        val formattedStatus = when (status) {
            is ConnectionStatus.Connected -> {
                val target = status.targetName.uppercase()
                getString(R.string.status_label, "\n" + getString(R.string.status_connected, target))
            }
            is ConnectionStatus.Connecting -> {
                val label = if (status.target.isNotEmpty()) "CONNECTING TO ${status.target}" else "CONNECTING"
                getString(R.string.status_label, label)
            }
            is ConnectionStatus.Error -> {
                getString(R.string.status_label, status.message.uppercase())
            }
            is ConnectionStatus.Disconnected -> {
                getString(R.string.status_label, getString(R.string.status_disconnected))
            }
        }

        binding.tvStatus.text = formattedStatus

        when (status) {
            is ConnectionStatus.Connected -> {
                binding.btnConnect.text = getString(R.string.btn_disconnect)
                binding.btnConnect.setTextColor(ContextCompat.getColor(this, R.color.cyber_red))
                binding.btnConnect.setStrokeColorResource(R.color.cyber_red)
                binding.btnConnect.isEnabled = true
            }
            is ConnectionStatus.Connecting -> {
                binding.btnConnect.text = getString(R.string.status_initializing)
                binding.btnConnect.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                binding.btnConnect.setStrokeColorResource(android.R.color.darker_gray)
                binding.btnConnect.isEnabled = false
            }
            is ConnectionStatus.Disconnected, is ConnectionStatus.Error -> {
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

    private fun checkPermissionsAndConnectBle() {
        val missingPermissions = permissionHelper.getMissingPermissions(this)
        if (missingPermissions.isEmpty()) {
            showDeviceSelectionDialog()
        } else {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    @SuppressLint("MissingPermission")
    private fun showDeviceSelectionDialog() {
        if (!deviceManager.isBluetoothSupported()) {
            Toast.makeText(this, getString(R.string.error_no_bluetooth), Toast.LENGTH_SHORT).show()
            return
        }
        if (!deviceManager.isBluetoothEnabled()) {
            Toast.makeText(this, getString(R.string.error_bt_disabled), Toast.LENGTH_SHORT).show()
            return
        }

        val deviceList = deviceManager.getBondedDevices()
        if (deviceList.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_targets), Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNames = deviceList.map { it.name ?: getString(R.string.error_no_targets) }.toTypedArray()

        AlertDialog.Builder(this, R.style.CyberDialogTheme)
            .setTitle(getString(R.string.dialog_select_target))
            .setItems(deviceNames) { _, which ->
                viewModel.connect(ConnectionTarget.Bluetooth(deviceList[which]))
            }
            .setNegativeButton(getString(R.string.dialog_abort), null)
            .show()
    }
}

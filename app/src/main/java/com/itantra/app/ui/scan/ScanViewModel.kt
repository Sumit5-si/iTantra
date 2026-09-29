package com.itantra.app.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.ITantraApplication
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DeviceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for the Scan Nearby Screen.
 *
 * Manages device discovery lifecycle over unified ConnectionManager (Wi-Fi Direct + Bluetooth Classic):
 *  IDLE → SCANNING (1-min timer) → DEVICES_FOUND | NO_DEVICES → CONNECTING → CONNECTED | FAILED
 */

enum class ScanState {
    IDLE,
    SCANNING,
    DEVICES_FOUND,
    NO_DEVICES,
    CONNECTING,
    CONNECTED,
    FAILED
}

class ScanViewModel : ViewModel() {

    private val app = ITantraApplication.instance
    private val connectionManager = app.connectionManager
    private val wifiTransport = app.wifiTransport
    private val bluetoothTransport = app.bluetoothTransport

    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<DeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _connectingDeviceId = MutableStateFlow<String?>(null)
    val connectingDeviceId: StateFlow<String?> = _connectingDeviceId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 1-minute (60 seconds) scan timer
    private val _scanTimerSeconds = MutableStateFlow(60)
    val scanTimerSeconds: StateFlow<Int> = _scanTimerSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var discoveryJob: Job? = null
    private var connectionStateJob: Job? = null

    init {
        // Observe connection state changes from unified ConnectionManager
        connectionStateJob = viewModelScope.launch {
            connectionManager.connectionState.collect { state ->
                when (state) {
                    ConnectionState.CONNECTED -> {
                        _scanState.value = ScanState.CONNECTED
                        _connectingDeviceId.value = null
                    }
                    ConnectionState.FAILED -> {
                        if (_scanState.value == ScanState.CONNECTING) {
                            _scanState.value = ScanState.FAILED
                            _connectingDeviceId.value = null
                            _errorMessage.value = "Connection failed. Please ensure the peer device accepts the connection."
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Check whether Wi-Fi or Bluetooth is enabled.
     */
    fun isWifiEnabled(): Boolean = wifiTransport.isWifiEnabled()
    fun isBluetoothEnabled(): Boolean = bluetoothTransport.isBluetoothEnabled()

    /**
     * Start scanning for nearby iTantra devices with a 1-minute (60 seconds) timer across Wi-Fi Direct and Bluetooth.
     */
    fun startScan() {
        _scanState.value = ScanState.SCANNING
        _errorMessage.value = null
        _scanTimerSeconds.value = 60

        // 1. Start 60-second countdown
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _scanTimerSeconds.value > 0 && _scanState.value == ScanState.SCANNING) {
                delay(1000)
                _scanTimerSeconds.value -= 1
            }
            if (_scanTimerSeconds.value == 0 && _scanState.value == ScanState.SCANNING) {
                stopScan()
                if (_discoveredDevices.value.isEmpty()) {
                    _scanState.value = ScanState.NO_DEVICES
                } else {
                    _scanState.value = ScanState.DEVICES_FOUND
                }
            }
        }

        // 2. Start unified peer discovery (Wi-Fi Direct + Bluetooth)
        discoveryJob?.cancel()
        discoveryJob = viewModelScope.launch {
            connectionManager.startDiscovery().collect { devices ->
                _discoveredDevices.value = devices
                if (devices.isNotEmpty()) {
                    _scanState.value = ScanState.DEVICES_FOUND
                }
            }
        }
    }

    /**
     * Stop scanning for devices and cancel timer.
     */
    fun stopScan() {
        timerJob?.cancel()
        timerJob = null
        discoveryJob?.cancel()
        discoveryJob = null

        viewModelScope.launch {
            connectionManager.stopDiscovery()
        }

        _scanState.value = if (_discoveredDevices.value.isNotEmpty()) {
            ScanState.DEVICES_FOUND
        } else {
            ScanState.IDLE
        }
    }

    /**
     * Connect to a selected device via Wi-Fi Direct or Bluetooth.
     */
    fun connectToDevice(device: DeviceInfo) {
        _scanState.value = ScanState.CONNECTING
        _connectingDeviceId.value = device.deviceId
        _errorMessage.value = null

        viewModelScope.launch {
            val result = connectionManager.connect(device)
            if (result.isFailure) {
                _scanState.value = ScanState.FAILED
                _connectingDeviceId.value = null
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to initiate connection"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        discoveryJob?.cancel()
        connectionStateJob?.cancel()
    }
}

package com.itantra.app.communication.transport

import com.itantra.app.ITantraApplication
import com.itantra.app.communication.bluetooth.BluetoothTransport
import com.itantra.app.communication.service.ITantraCommunicationService
import com.itantra.app.communication.wifi.WifiTransport
import com.itantra.app.core.logging.ITantraLogger
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.domain.model.DeviceInfo
import com.itantra.app.domain.model.MessagePacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Connection manager that orchestrates transport selection and lifecycle.
 *
 * Uses Wi-Fi Direct as the primary local high-bandwidth transport,
 * and seamlessly supports Bluetooth Classic RFCOMM as fallback.
 * Merges discovery lists and packet streams so UI/view models interact
 * with a single unified offline communication interface.
 */
class ConnectionManager(
    val wifiTransport: WifiTransport,
    val bluetoothTransport: BluetoothTransport
) {
    companion object {
        private const val TAG = "ConnMgr"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeTransport = MutableStateFlow<MessageTransport?>(null)
    val activeTransport: MessageTransport? get() = _activeTransport.value

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private val _connectionType = MutableStateFlow<ConnectionType?>(null)
    val connectionType: StateFlow<ConnectionType?> = _connectionType.asStateFlow()

    init {
        // Observe Wi-Fi Direct state
        scope.launch {
            wifiTransport.connectionState.collect { wifiState ->
                if (wifiState == ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                    _activeTransport.value = wifiTransport
                    _connectedDevice.value = wifiTransport.connectedDevice.value
                    _connectionType.value = ConnectionType.WIFI_DIRECT
                    ITantraLogger.i(TAG, "Active link set to Wi-Fi Direct")
                    try {
                        ITantraCommunicationService.startService(
                            ITantraApplication.instance,
                            _connectedDevice.value?.deviceName
                        )
                    } catch (e: Exception) {
                        ITantraLogger.w(TAG, "Could not start foreground service: ${e.message}")
                    }
                } else if (_connectionType.value == ConnectionType.WIFI_DIRECT && wifiState != ConnectionState.CONNECTING) {
                    if (bluetoothTransport.connectionState.value == ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.CONNECTED
                        _activeTransport.value = bluetoothTransport
                        _connectedDevice.value = bluetoothTransport.connectedDevice.value
                        _connectionType.value = ConnectionType.BLUETOOTH
                    } else {
                        _connectionState.value = wifiState
                        _activeTransport.value = null
                        _connectedDevice.value = null
                        _connectionType.value = null
                        try {
                            ITantraCommunicationService.stopService(ITantraApplication.instance)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Observe Bluetooth state
        scope.launch {
            bluetoothTransport.connectionState.collect { btState ->
                if (btState == ConnectionState.CONNECTED && _connectionState.value != ConnectionState.CONNECTED) {
                    _connectionState.value = ConnectionState.CONNECTED
                    _activeTransport.value = bluetoothTransport
                    _connectedDevice.value = bluetoothTransport.connectedDevice.value
                    _connectionType.value = ConnectionType.BLUETOOTH
                    ITantraLogger.i(TAG, "Active link set to Bluetooth RFCOMM")
                    try {
                        ITantraCommunicationService.startService(
                            ITantraApplication.instance,
                            _connectedDevice.value?.deviceName
                        )
                    } catch (e: Exception) {
                        ITantraLogger.w(TAG, "Could not start foreground service: ${e.message}")
                    }
                } else if (_connectionType.value == ConnectionType.BLUETOOTH && btState != ConnectionState.CONNECTING) {
                    if (wifiTransport.connectionState.value == ConnectionState.CONNECTED) {
                        _connectionState.value = ConnectionState.CONNECTED
                        _activeTransport.value = wifiTransport
                        _connectedDevice.value = wifiTransport.connectedDevice.value
                        _connectionType.value = ConnectionType.WIFI_DIRECT
                    } else {
                        _connectionState.value = btState
                        _activeTransport.value = null
                        _connectedDevice.value = null
                        _connectionType.value = null
                        try {
                            ITantraCommunicationService.stopService(ITantraApplication.instance)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    /**
     * Start unified device discovery across both Wi-Fi Direct and Bluetooth.
     */
    suspend fun startDiscovery(): Flow<List<DeviceInfo>> {
        _connectionState.value = ConnectionState.DISCOVERING
        ITantraLogger.i(TAG, "Starting unified peer discovery (Wi-Fi Direct + Bluetooth)...")

        return combine(
            wifiTransport.startDiscovery(),
            bluetoothTransport.startDiscovery()
        ) { wifiDevices, btDevices ->
            val mergedMap = LinkedHashMap<String, DeviceInfo>()
            wifiDevices.forEach { mergedMap[it.deviceId] = it }
            btDevices.forEach { mergedMap[it.deviceId] = it }
            mergedMap.values.toList()
        }
    }

    suspend fun stopDiscovery() {
        try {
            wifiTransport.stopDiscovery()
            bluetoothTransport.stopDiscovery()
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Error stopping discovery: ${e.message}")
        }
        if (_connectionState.value == ConnectionState.DISCOVERING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Connect to a specific peer based on its connection type.
     */
    suspend fun connect(device: DeviceInfo): Result<Unit> {
        _connectionState.value = ConnectionState.CONNECTING
        ITantraLogger.i(TAG, "Connecting to ${device.deviceName} via ${device.connectionType}...")

        return when (device.connectionType) {
            ConnectionType.WIFI_DIRECT -> {
                val result = wifiTransport.connect(device)
                if (result.isSuccess) {
                    _activeTransport.value = wifiTransport
                    _connectedDevice.value = device
                    _connectionType.value = ConnectionType.WIFI_DIRECT
                    _connectionState.value = ConnectionState.CONNECTED
                } else {
                    _connectionState.value = ConnectionState.FAILED
                }
                result
            }
            ConnectionType.BLUETOOTH -> {
                val result = bluetoothTransport.connect(device)
                if (result.isSuccess) {
                    _activeTransport.value = bluetoothTransport
                    _connectedDevice.value = device
                    _connectionType.value = ConnectionType.BLUETOOTH
                    _connectionState.value = ConnectionState.CONNECTED
                } else {
                    _connectionState.value = ConnectionState.FAILED
                }
                result
            }
        }
    }

    suspend fun disconnect() {
        try {
            wifiTransport.disconnect()
            bluetoothTransport.disconnect()
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Error disconnecting: ${e.message}")
        }
        try {
            ITantraCommunicationService.stopService(ITantraApplication.instance)
        } catch (_: Exception) {}
        _activeTransport.value = null
        _connectedDevice.value = null
        _connectionType.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
        ITantraLogger.i(TAG, "Disconnected all transports")
    }

    suspend fun send(packet: MessagePacket): Result<Unit> {
        val transport = _activeTransport.value
            ?: if (wifiTransport.connectionState.value == ConnectionState.CONNECTED) wifiTransport
               else if (bluetoothTransport.connectionState.value == ConnectionState.CONNECTED) bluetoothTransport
               else null

        if (transport == null) {
            return Result.failure(IllegalStateException("No active transport connected"))
        }

        return transport.send(packet)
    }

    /**
     * Stream incoming packets from both Wi-Fi Direct and Bluetooth.
     */
    fun receive(): Flow<MessagePacket> {
        return merge(
            wifiTransport.receive(),
            bluetoothTransport.receive()
        )
    }
}

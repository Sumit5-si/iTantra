package com.itantra.app.communication.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.itantra.app.communication.packet.PacketSerializer
import com.itantra.app.communication.transport.MessageTransport
import com.itantra.app.core.logging.ITantraLogger
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.ConnectionType
import com.itantra.app.domain.model.DeviceInfo
import com.itantra.app.domain.model.MessagePacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Bluetooth Classic RFCOMM transport — Fallback transport when Wi-Fi Direct is unavailable.
 *
 * Implements:
 *  - Standard Serial Port Profile (SPP) RFCOMM socket
 *  - Background server socket listener for incoming pairing/connections
 *  - Client connection to remote Bluetooth devices
 *  - Compact length-prefixed JSON packet serialization identical to Wi-Fi Direct
 *  - Strict zero-cloud offline constraints
 */
class BluetoothTransport(
    private val context: Context
) : MessageTransport {

    companion object {
        private const val TAG = "BluetoothTransport"
        private const val SERVICE_NAME = "iTantra_RFCOMM"
        // Standard Bluetooth Serial Port Profile (SPP) UUID
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val transportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var dataOutputStream: DataOutputStream? = null
    private var dataInputStream: DataInputStream? = null

    private val _incomingPackets = MutableSharedFlow<MessagePacket>(replay = 0, extraBufferCapacity = 64)
    private var receiverJob: Job? = null
    private var serverJob: Job? = null

    init {
        startServerListener()
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun isAvailable(): Boolean = isBluetoothEnabled()

    /**
     * Start background RFCOMM server to accept incoming connections from other iTantra devices.
     */
    fun startServerListener() {
        if (serverJob?.isActive == true) return

        serverJob = transportScope.launch {
            while (isActive) {
                try {
                    val adapter = bluetoothAdapter
                    if (adapter == null || !adapter.isEnabled || !hasBluetoothPermission()) {
                        delay(4000)
                        continue
                    }

                    if (_connectionState.value == ConnectionState.CONNECTED) {
                        delay(2000)
                        continue
                    }

                    ITantraLogger.i(TAG, "Starting RFCOMM server socket listener...")
                    val server = try {
                        adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
                    } catch (e: SecurityException) {
                        ITantraLogger.w(TAG, "SecurityException creating RFCOMM server: ${e.message}")
                        delay(4000)
                        continue
                    }

                    serverSocket = server
                    val socket = try {
                        server.accept()
                    } catch (e: IOException) {
                        null
                    }

                    try {
                        server.close()
                    } catch (_: Exception) {}
                    serverSocket = null

                    if (socket != null && isActive) {
                        ITantraLogger.i(TAG, "Incoming Bluetooth RFCOMM connection accepted!")
                        handleConnectedSocket(socket, socket.remoteDevice)
                        // Hold here while connection is active
                        while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                            delay(1000)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        ITantraLogger.w(TAG, "RFCOMM server loop error: ${e.message}")
                        delay(3000)
                    }
                }
            }
        }
    }

    override suspend fun startDiscovery(): Flow<List<DeviceInfo>> = callbackFlow {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled || !hasBluetoothPermission()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val discoveredMap = LinkedHashMap<String, DeviceInfo>()

        // 1. Instantly populate bonded (paired) devices
        try {
            adapter.bondedDevices?.forEach { device ->
                val name = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
                discoveredMap[device.address] = DeviceInfo(
                    deviceId = device.address,
                    deviceName = name,
                    deviceAddress = device.address,
                    connectionType = ConnectionType.BLUETOOTH
                )
            }
        } catch (e: SecurityException) {
            ITantraLogger.w(TAG, "SecurityException reading bonded devices: ${e.message}")
        }

        trySend(discoveredMap.values.toList())

        // 2. Discover unbonded nearby devices
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                        if (device != null) {
                            val address = device.address
                            val name = try { device.name ?: address } catch (_: SecurityException) { address }
                            discoveredMap[address] = DeviceInfo(
                                deviceId = address,
                                deviceName = name,
                                deviceAddress = address,
                                connectionType = ConnectionType.BLUETOOTH
                            )
                            trySend(discoveredMap.values.toList())
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        ITantraLogger.i(TAG, "Bluetooth discovery finished. Found ${discoveredMap.size} devices.")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        try {
            context.registerReceiver(receiver, filter)
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
            ITantraLogger.i(TAG, "Bluetooth discovery started")
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Failed to start Bluetooth discovery: ${e.message}")
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            } catch (_: Exception) {}
        }
    }

    override suspend fun stopDiscovery() {
        try {
            if (hasBluetoothPermission() && bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }
        } catch (_: Exception) {}
    }

    override suspend fun connect(device: DeviceInfo): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth adapter not available"))

        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is turned off"))
        }

        if (!hasBluetoothPermission()) {
            return@withContext Result.failure(SecurityException("Bluetooth connect permission not granted"))
        }

        _connectionState.value = ConnectionState.CONNECTING
        stopDiscovery()

        try {
            val remoteDevice = adapter.getRemoteDevice(device.deviceId)
            val socket = remoteDevice.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            handleConnectedSocket(socket, remoteDevice)
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.FAILED
            ITantraLogger.e(TAG, "Failed to connect to Bluetooth device ${device.deviceName}: ${e.message}")
            Result.failure(e)
        }
    }

    private fun handleConnectedSocket(socket: BluetoothSocket, device: BluetoothDevice?) {
        try {
            activeSocket?.close()
        } catch (_: Exception) {}

        activeSocket = socket
        dataOutputStream = DataOutputStream(socket.outputStream)
        dataInputStream = DataInputStream(socket.inputStream)

        val deviceName = try {
            device?.name ?: device?.address ?: "Bluetooth Peer"
        } catch (_: SecurityException) {
            device?.address ?: "Bluetooth Peer"
        }

        _connectedDevice.value = DeviceInfo(
            deviceId = device?.address ?: "bt_peer",
            deviceName = deviceName,
            deviceAddress = device?.address ?: "00:00:00:00:00:00",
            connectionType = ConnectionType.BLUETOOTH
        )
        _connectionState.value = ConnectionState.CONNECTED
        ITantraLogger.i(TAG, "Bluetooth RFCOMM link connected to $deviceName (${device?.address})")

        startPacketReceiver()
    }

    private fun startPacketReceiver() {
        receiverJob?.cancel()
        receiverJob = transportScope.launch {
            val input = dataInputStream ?: return@launch
            ITantraLogger.i(TAG, "Bluetooth continuous packet receiver active")
            while (isActive) {
                try {
                    val header = ByteArray(4)
                    input.readFully(header)
                    val length = PacketSerializer.readLength(header)
                    if (length <= 0 || length > 65536) {
                        ITantraLogger.w(TAG, "Invalid Bluetooth packet length: $length")
                        continue
                    }

                    val payload = ByteArray(length)
                    input.readFully(payload)

                    val packet = PacketSerializer.deserialize(payload)
                    ITantraLogger.i(TAG, "Bluetooth packet received: id=${packet.messageId}, type=${packet.messageType}, text='${packet.payload}'")
                    _incomingPackets.emit(packet)
                } catch (e: Exception) {
                    if (isActive) {
                        ITantraLogger.w(TAG, "Bluetooth packet receiver disconnected: ${e.message}")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        _connectedDevice.value = null
                    }
                    break
                }
            }
        }
    }

    override suspend fun send(packet: MessagePacket): Result<Unit> = withContext(Dispatchers.IO) {
        val out = dataOutputStream
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth socket is not connected"))

        try {
            val serialized = PacketSerializer.serialize(packet)
            out.write(serialized)
            out.flush()
            ITantraLogger.i(TAG, "Bluetooth packet sent: id=${packet.messageId}, size=${serialized.size}B")
            Result.success(Unit)
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Failed to send Bluetooth packet: ${e.message}")
            Result.failure(e)
        }
    }

    override fun receive(): Flow<MessagePacket> = _incomingPackets.asSharedFlow()

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        receiverJob?.cancel()
        receiverJob = null
        try {
            dataInputStream?.close()
            dataOutputStream?.close()
            activeSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Error closing Bluetooth transport: ${e.message}")
        }
        dataInputStream = null
        dataOutputStream = null
        activeSocket = null
        serverSocket = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
        ITantraLogger.i(TAG, "Bluetooth transport disconnected")
    }
}

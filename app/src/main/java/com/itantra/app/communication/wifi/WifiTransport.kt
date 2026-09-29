package com.itantra.app.communication.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
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
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Wi-Fi Direct transport implementation.
 *
 * Uses WifiP2pManager for peer discovery and group formation.
 * Once connected, establishes a TCP socket for bidirectional messaging:
 *  - Group owner runs a ServerSocket
 *  - Client connects to the group owner's IP
 *
 * Per project rules:
 *  - Primary transport: Wi-Fi Direct (WifiP2pManager) raw TCP socket
 *  - Text-only payloads, never raw audio
 *  - Packets < 256 bytes (compact JSON)
 */
class WifiTransport(
    private val context: Context
) : MessageTransport {

    companion object {
        private const val TAG = "WifiTransport"
        private const val PORT = 8765
        private const val SOCKET_TIMEOUT_MS = 15_000
    }

    private val transportScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DeviceInfo?>(null)
    val connectedDevice: StateFlow<DeviceInfo?> = _connectedDevice.asStateFlow()

    private val wifiP2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager

    private var p2pChannel: WifiP2pManager.Channel? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var dataOutputStream: DataOutputStream? = null
    private var dataInputStream: DataInputStream? = null
    private var isGroupOwner = false

    private val _incomingPackets = MutableSharedFlow<MessagePacket>(replay = 0, extraBufferCapacity = 64)
    private var receiverJob: Job? = null

    init {
        initChannel()
        registerConnectionReceiver()
    }

    private fun initChannel() {
        try {
            p2pChannel = wifiP2pManager?.initialize(context, context.mainLooper) {
                ITantraLogger.w(TAG, "Wi-Fi P2P channel disconnected; re-initializing...")
                initChannel()
            }
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Failed to initialize Wi-Fi P2P channel", e)
        }
    }

    /**
     * Check if Wi-Fi hardware is currently enabled on the device.
     */
    fun isWifiEnabled(): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        return wifiManager?.isWifiEnabled == true
    }

    private fun registerConnectionReceiver() {
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        @Suppress("DEPRECATION")
                        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (networkInfo?.isConnected == true) {
                            handleConnectionEstablished()
                        } else {
                            if (_connectionState.value == ConnectionState.CONNECTED) {
                                _connectionState.value = ConnectionState.DISCONNECTED
                            }
                        }
                    }
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                            ITantraLogger.w(TAG, "Wi-Fi P2P not enabled on device")
                        }
                    }
                }
            }
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Failed to register connection receiver: ${e.message}")
        }
    }

    private fun handleConnectionEstablished() {
        val channel = p2pChannel ?: run {
            initChannel()
            p2pChannel
        } ?: return

        wifiP2pManager?.requestConnectionInfo(channel) { info: WifiP2pInfo? ->
            ITantraLogger.i(TAG, "requestConnectionInfo: groupFormed=${info?.groupFormed}, isGO=${info?.isGroupOwner}, host=${info?.groupOwnerAddress?.hostAddress}")
            if (info != null && info.groupFormed) {
                isGroupOwner = info.isGroupOwner
                val goHost = info.groupOwnerAddress?.hostAddress

                if (_connectedDevice.value == null) {
                    _connectedDevice.value = DeviceInfo(
                        deviceId = if (isGroupOwner) "client_peer" else "host_peer",
                        deviceName = if (isGroupOwner) "Connected Client" else "Connected Host",
                        deviceAddress = goHost ?: "Wi-Fi Direct P2P",
                        connectionType = ConnectionType.WIFI_DIRECT,
                        isAvailable = true
                    )
                }

                transportScope.launch {
                    try {
                        if (isGroupOwner) {
                            setupServer()
                        } else {
                            val targetHost = if (!goHost.isNullOrBlank()) goHost else "192.168.49.1"
                            setupClientWithRetry(targetHost)
                        }
                        _connectionState.value = ConnectionState.CONNECTED
                        ITantraLogger.i(TAG, "Wi-Fi Direct TCP link fully established! isGroupOwner=$isGroupOwner")
                    } catch (e: Exception) {
                        ITantraLogger.e(TAG, "Socket setup failed", e)
                        _connectionState.value = ConnectionState.FAILED
                    }
                }
            }
        }
    }

    override suspend fun startDiscovery(): Flow<List<DeviceInfo>> = callbackFlow {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            _connectionState.value = ConnectionState.DISCOVERING
        }

        val peerListener = WifiP2pManager.PeerListListener { peers: WifiP2pDeviceList? ->
            val devices = peers?.deviceList?.map { device ->
                DeviceInfo(
                    deviceId = device.deviceAddress,
                    deviceName = device.deviceName.ifEmpty { "iTantra Device" },
                    deviceAddress = device.deviceAddress,
                    connectionType = ConnectionType.WIFI_DIRECT,
                    isAvailable = device.status == WifiP2pDevice.AVAILABLE
                )
            } ?: emptyList()

            trySend(devices)
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        p2pChannel?.let { channel ->
                            try {
                                wifiP2pManager?.requestPeers(channel, peerListener)
                            } catch (e: SecurityException) {
                                ITantraLogger.w(TAG, "Permission missing for requestPeers: ${e.message}")
                            } catch (e: Exception) {
                                ITantraLogger.w(TAG, "Error requesting peers: ${e.message}")
                            }
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        }
        try {
            context.registerReceiver(receiver, intentFilter)
        } catch (e: Exception) {
            ITantraLogger.w(TAG, "Failed to register peer receiver: ${e.message}")
        }

        fun executeDiscovery(retryCount: Int = 0) {
            val channel = p2pChannel ?: run {
                initChannel()
                p2pChannel
            }
            if (channel == null || wifiP2pManager == null) {
                ITantraLogger.w(TAG, "Cannot start discovery: WifiP2pManager or channel is null")
                return
            }

            try {
                wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        ITantraLogger.i(TAG, "Wi-Fi Direct peer discovery started successfully")
                    }

                    override fun onFailure(reason: Int) {
                        val reasonText = when (reason) {
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported"
                            WifiP2pManager.BUSY -> "Framework busy"
                            WifiP2pManager.ERROR -> "Internal error"
                            else -> "Reason code $reason"
                        }
                        ITantraLogger.w(TAG, "Peer discovery failed: $reasonText ($reason)")
                        if (reason == WifiP2pManager.BUSY && retryCount < 2) {
                            transportScope.launch {
                                delay(1000)
                                executeDiscovery(retryCount + 1)
                            }
                        }
                        // NOTE: NEVER set _connectionState = ConnectionState.FAILED here!
                        // Discovery failure is not a socket/peer connection failure.
                    }
                })
            } catch (e: SecurityException) {
                ITantraLogger.w(TAG, "SecurityException starting discovery: ${e.message}")
            } catch (e: Exception) {
                ITantraLogger.e(TAG, "Exception starting discovery: ${e.message}")
            }
        }

        executeDiscovery()

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            p2pChannel?.let { channel ->
                try {
                    wifiP2pManager?.stopPeerDiscovery(channel, null)
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun stopDiscovery() {
        p2pChannel?.let { channel ->
            try {
                wifiP2pManager?.stopPeerDiscovery(channel, null)
            } catch (_: Exception) {}
        }
        if (_connectionState.value == ConnectionState.DISCOVERING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    override suspend fun connect(device: DeviceInfo): Result<Unit> = withContext(Dispatchers.IO) {
        val channel = p2pChannel ?: run {
            initChannel()
            p2pChannel
        } ?: return@withContext Result.failure(IllegalStateException("Wi-Fi Direct channel unavailable"))

        try {
            _connectionState.value = ConnectionState.CONNECTING
            _connectedDevice.value = device

            val config = WifiP2pConfig().apply {
                deviceAddress = device.deviceAddress
                wps.setup = WpsInfo.PBC
            }

            // Connect via Wi-Fi Direct
            wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    ITantraLogger.i(TAG, "Wi-Fi Direct connect initiated to ${device.deviceName}")
                }

                override fun onFailure(reason: Int) {
                    ITantraLogger.e(TAG, "Wi-Fi Direct connect failed: reason=$reason")
                    _connectionState.value = ConnectionState.FAILED
                }
            })

            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.FAILED
            Result.failure(e)
        }
    }

    private suspend fun setupServer() = withContext(Dispatchers.IO) {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}

        val sSocket = ServerSocket().apply {
            reuseAddress = true
            bind(InetSocketAddress(PORT))
        }
        serverSocket = sSocket
        ITantraLogger.i(TAG, "Server socket bound on port $PORT, waiting for client accept...")

        val socket = sSocket.accept()
        socket.tcpNoDelay = true
        socket.soTimeout = 0 // Infinite read timeout for continuous packet reception
        clientSocket = socket
        setupStreams()
        startPacketReceiver()
        ITantraLogger.i(TAG, "Server accepted client connection from ${socket.remoteSocketAddress}")
    }

    private suspend fun setupClientWithRetry(targetHost: String) = withContext(Dispatchers.IO) {
        try {
            clientSocket?.close()
        } catch (_: Exception) {}

        var connected = false
        var attempts = 0
        var lastError: Exception? = null

        // Give Group Owner up to 15 attempts (~15 seconds) to bind and listen
        while (!connected && attempts < 15) {
            attempts++
            try {
                ITantraLogger.i(TAG, "Connecting to GO at $targetHost:$PORT (attempt $attempts)...")
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.soTimeout = 0 // Continuous read
                socket.connect(InetSocketAddress(targetHost, PORT), 2000)
                clientSocket = socket
                setupStreams()
                startPacketReceiver()
                connected = true
                ITantraLogger.i(TAG, "Client socket connected to $targetHost:$PORT on attempt $attempts")
            } catch (e: Exception) {
                lastError = e
                ITantraLogger.w(TAG, "Client connect attempt $attempts failed: ${e.message}")
                delay(1000)
            }
        }

        if (!connected) {
            throw (lastError ?: IOException("Failed to connect to group owner after $attempts attempts"))
        }
    }

    private fun setupStreams() {
        clientSocket?.let { socket ->
            dataOutputStream = DataOutputStream(socket.getOutputStream())
            dataInputStream = DataInputStream(socket.getInputStream())
        }
    }

    private fun startPacketReceiver() {
        receiverJob?.cancel()
        receiverJob = transportScope.launch {
            val input = dataInputStream ?: return@launch
            ITantraLogger.i(TAG, "Background continuous packet receiver started")
            while (isActive) {
                try {
                    // Read 4-byte length prefix
                    val header = ByteArray(4)
                    input.readFully(header)
                    val length = PacketSerializer.readLength(header)
                    if (length <= 0 || length > 65536) {
                        ITantraLogger.w(TAG, "Invalid packet length header: $length")
                        continue
                    }

                    // Read JSON payload
                    val payload = ByteArray(length)
                    input.readFully(payload)

                    val packet = PacketSerializer.deserialize(payload)
                    ITantraLogger.i(TAG, "Packet received from link: id=${packet.messageId}, type=${packet.messageType}, text='${packet.payload}'")
                    _incomingPackets.emit(packet)
                } catch (e: Exception) {
                    if (isActive) {
                        ITantraLogger.w(TAG, "Receiver loop terminated: ${e.message}")
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                    break
                }
            }
        }
    }

    override suspend fun disconnect(): Unit = withContext(Dispatchers.IO) {
        receiverJob?.cancel()
        receiverJob = null
        try {
            dataInputStream?.close()
            dataOutputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Error during disconnect", e)
        }
        dataInputStream = null
        dataOutputStream = null
        clientSocket = null
        serverSocket = null
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
        isGroupOwner = false

        // Comprehensive teardown of Wi-Fi Direct in Android System Settings
        p2pChannel?.let { channel ->
            try {
                wifiP2pManager?.removeGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        ITantraLogger.i(TAG, "Wi-Fi Direct removeGroup SUCCESS: completely detached from system settings")
                    }
                    override fun onFailure(reason: Int) {
                        ITantraLogger.w(TAG, "Wi-Fi Direct removeGroup failure (reason=$reason), trying cancelConnect")
                        try {
                            wifiP2pManager?.cancelConnect(channel, null)
                        } catch (_: Exception) {}
                    }
                })
                wifiP2pManager?.cancelConnect(channel, null)
                wifiP2pManager?.stopPeerDiscovery(channel, null)
            } catch (e: Exception) {
                ITantraLogger.e(TAG, "Error cleaning up Wi-Fi Direct group: ${e.message}")
            }
        }
        Unit
    }

    /**
     * Synchronous force disconnect for app termination / onTaskRemoved.
     */
    fun forceSystemDisconnect() {
        try {
            dataInputStream?.close()
            dataOutputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (_: Exception) {}
        p2pChannel?.let { channel ->
            try {
                wifiP2pManager?.removeGroup(channel, null)
                wifiP2pManager?.cancelConnect(channel, null)
                wifiP2pManager?.stopPeerDiscovery(channel, null)
            } catch (_: Exception) {}
        }
        isGroupOwner = false
        _connectedDevice.value = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override suspend fun send(packet: MessagePacket): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val output = dataOutputStream
                ?: return@withContext Result.failure(IllegalStateException("Not connected: output stream is null"))

            val data = PacketSerializer.serialize(packet)
            output.write(data)
            output.flush()

            ITantraLogger.i(TAG, "Sent packet over Wi-Fi Direct: id=${packet.messageId}, size=${data.size} bytes, text='${packet.payload}'")
            Result.success(Unit)
        } catch (e: Exception) {
            ITantraLogger.e(TAG, "Send failed", e)
            Result.failure(e)
        }
    }

    override fun receive(): Flow<MessagePacket> = _incomingPackets.asSharedFlow()

    override fun isAvailable(): Boolean {
        return wifiP2pManager != null
    }
}

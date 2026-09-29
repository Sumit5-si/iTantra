package com.itantra.app.communication.transport

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DeviceInfo
import com.itantra.app.domain.model.MessagePacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport interface for device-to-device communication.
 *
 * Implementations:
 *  - WifiTransport: Wi-Fi Direct via WifiP2pManager + TCP sockets
 *  - BluetoothTransport: Bluetooth Classic RFCOMM sockets
 *
 * The UI layer NEVER interacts with sockets directly.
 * Flow: UI → ViewModel → UseCase → Repository → MessageTransport → Socket
 */
interface MessageTransport {

    /** Current connection state as observable flow. */
    val connectionState: StateFlow<ConnectionState>

    /** Start discovering nearby devices. Returns a flow of discovered devices. */
    suspend fun startDiscovery(): Flow<List<DeviceInfo>>

    /** Stop device discovery. */
    suspend fun stopDiscovery()

    /** Connect to a specific device. */
    suspend fun connect(device: DeviceInfo): Result<Unit>

    /** Disconnect from the current device. */
    suspend fun disconnect()

    /** Send a message packet to the connected device. */
    suspend fun send(packet: MessagePacket): Result<Unit>

    /** Receive message packets from the connected device. */
    fun receive(): Flow<MessagePacket>

    /** Check if transport is available on this device. */
    fun isAvailable(): Boolean
}

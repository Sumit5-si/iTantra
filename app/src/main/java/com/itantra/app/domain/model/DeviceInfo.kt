package com.itantra.app.domain.model

/**
 * Represents a discovered nearby iTantra device.
 */
data class DeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val deviceAddress: String,
    val connectionType: ConnectionType,
    val signalStrength: Int? = null,
    val isAvailable: Boolean = true
)

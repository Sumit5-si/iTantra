package com.itantra.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Centralized permission management for iTantra.
 *
 * Permissions are requested contextually — not all on first launch:
 *  - Microphone: when user first activates voice feature
 *  - Location/Nearby: when user first scans for devices
 *  - Bluetooth: when user selects Bluetooth transport
 */
object PermissionManager {

    /**
     * Permissions required for Wi-Fi Direct device discovery.
     * API 33+: NEARBY_WIFI_DEVICES replaces location for peer discovery.
     */
    fun getDiscoveryPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        return permissions.toTypedArray()
    }

    /**
     * Permissions required for microphone/voice features.
     */
    fun getMicrophonePermissions(): Array<String> {
        return arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Permissions required for Bluetooth transport.
     */
    fun getBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    /**
     * Check if all specified permissions are granted.
     */
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if microphone permission is granted.
     */
    fun isMicrophoneGranted(context: Context): Boolean {
        return arePermissionsGranted(context, getMicrophonePermissions())
    }

    /**
     * Check if discovery permissions are granted.
     * On Android 13+ (API 33), NEARBY_WIFI_DEVICES is the primary permission.
     */
    fun isDiscoveryGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if Bluetooth permissions are granted.
     */
    fun isBluetoothGranted(context: Context): Boolean {
        return arePermissionsGranted(context, getBluetoothPermissions())
    }
}

package com.itantra.app.ui.settings

import androidx.lifecycle.ViewModel
import com.itantra.app.ui.theme.ThemeManager
import com.itantra.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = ThemeManager.themeMode

    // Permission states
    private val _micPermissionGranted = MutableStateFlow(false)
    val micPermissionGranted: StateFlow<Boolean> = _micPermissionGranted.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    private val _bluetoothPermissionGranted = MutableStateFlow(false)
    val bluetoothPermissionGranted: StateFlow<Boolean> = _bluetoothPermissionGranted.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        ThemeManager.setThemeMode(mode)
    }

    fun updatePermissionState(mic: Boolean, location: Boolean, bluetooth: Boolean) {
        _micPermissionGranted.value = mic
        _locationPermissionGranted.value = location
        _bluetoothPermissionGranted.value = bluetooth
    }
}

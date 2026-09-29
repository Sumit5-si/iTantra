package com.itantra.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton Theme Manager that stores and broadcasts the current ThemeMode (Light, Dark, System).
 * Persists selection in SharedPreferences so user choice is preserved across app sessions.
 */
object ThemeManager {
    private const val PREFS_NAME = "itantra_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    private var sharedPreferences: SharedPreferences? = null

    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences = prefs
        val savedTheme = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        _themeMode.value = try {
            ThemeMode.valueOf(savedTheme)
        } catch (_: Exception) {
            ThemeMode.LIGHT
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPreferences?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }
}

package com.itantra.app.core.localization

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.itantra.app.domain.model.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Centralized language management for iTantra.
 *
 * Controls:
 *  - STT language/model selection
 *  - TTS language/voice selection
 *  - UI language (where supported)
 *  - Message language metadata
 *
 * Language preference is persisted via DataStore.
 * Do NOT hard-code language behavior inside individual screens.
 */

private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "itantra_language_prefs"
)

class LanguageManager(private val context: Context) {

    companion object {
        private val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    /**
     * Observe the currently selected language as a Flow.
     */
    val selectedLanguage: Flow<Language> = context.languageDataStore.data.map { prefs ->
        val code = prefs[KEY_SELECTED_LANGUAGE] ?: Language.DEFAULT.code
        Language.fromCode(code) ?: Language.DEFAULT
    }

    /**
     * Update the selected language. Persisted immediately.
     */
    suspend fun setLanguage(language: Language) {
        context.languageDataStore.edit { prefs ->
            prefs[KEY_SELECTED_LANGUAGE] = language.code
        }
    }

    /**
     * Get all supported languages.
     */
    fun getSupportedLanguages(): List<Language> = Language.entries.toList()
}

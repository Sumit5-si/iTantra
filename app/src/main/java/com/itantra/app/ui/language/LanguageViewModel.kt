package com.itantra.app.ui.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.ITantraApplication
import com.itantra.app.domain.model.Language
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguageViewModel : ViewModel() {

    private val languageManager = ITantraApplication.instance.languageManager

    val selectedLanguage: StateFlow<Language> = languageManager.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Language.DEFAULT)

    val supportedLanguages: List<Language> = languageManager.getSupportedLanguages()

    fun selectLanguage(language: Language) {
        viewModelScope.launch {
            languageManager.setLanguage(language)
        }
    }
}

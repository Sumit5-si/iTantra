package com.itantra.app.domain.model

/**
 * Supported languages for iTantra voice communication.
 *
 * Each language has:
 *  - code: BCP 47 language tag used in message metadata, STT/TTS engine selection
 *  - displayName: English name for UI
 *  - nativeName: Name in the language's own script
 *
 * Strict 10 languages per project rules.
 */
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String
) {
    HINDI("hi", "Hindi", "हिन्दी"),
    ENGLISH("en", "English", "English"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    MARATHI("mr", "Marathi", "मराठी"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ"),
    MALAYALAM("ml", "Malayalam", "മലയാളം"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    TELUGU("te", "Telugu", "తెలుగు"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ"),
    BENGALI("bn", "Bengali", "বাংলা");

    companion object {
        fun fromCode(code: String): Language? =
            entries.find { it.code == code }

        val DEFAULT = HINDI
    }
}

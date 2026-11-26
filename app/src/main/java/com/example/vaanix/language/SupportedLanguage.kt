package com.example.vaanix.language

import java.util.Locale

/**
 * Pan-India language definition model.
 * Extensible data structure (not a closed enum) so regional dialects and
 * additional Indian languages can be registered without modifying core logic.
 */
data class SupportedLanguage(
    val id: String,
    val bcp47Tag: String,
    val englishName: String,
    val nativeName: String,
    val scriptName: String,
    val mixedVariantLabel: String,
    val defaultOfflineExpected: Boolean,
    val knownLimitationsNote: String,
    val fallbackTag: String = "en-IN"
) {
    fun toLocale(): Locale = Locale.forLanguageTag(bcp47Tag)
}

object LanguageCatalog {

    val TELUGU = SupportedLanguage(
        id = "telugu",
        bcp47Tag = "te-IN",
        englishName = "Telugu",
        nativeName = "తెలుగు",
        scriptName = "Telugu",
        mixedVariantLabel = "Tenglish (Telugu + English)",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Offline voice packs vary by device. Mixed Tenglish works best with clear speech."
    )

    val HINDI = SupportedLanguage(
        id = "hindi",
        bcp47Tag = "hi-IN",
        englishName = "Hindi",
        nativeName = "हिन्दी",
        scriptName = "Devanagari",
        mixedVariantLabel = "Hinglish (Hindi + English)",
        defaultOfflineExpected = true,
        knownLimitationsNote = "Requires device speech engine for voice input; offline accuracy depends on installed Google speech pack."
    )

    val TAMIL = SupportedLanguage(
        id = "tamil",
        bcp47Tag = "ta-IN",
        englishName = "Tamil",
        nativeName = "தமிழ்",
        scriptName = "Tamil",
        mixedVariantLabel = "Tanglish (Tamil + English)",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Colloquial regional accents may need slower, clearer speech on some Android devices."
    )

    val KANNADA = SupportedLanguage(
        id = "kannada",
        bcp47Tag = "kn-IN",
        englishName = "Kannada",
        nativeName = "ಕನ್ನಡ",
        scriptName = "Kannada",
        mixedVariantLabel = "Kanglish (Kannada + English)",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Device TTS voice data may need one-time download in Android Text-to-Speech settings."
    )

    val MALAYALAM = SupportedLanguage(
        id = "malayalam",
        bcp47Tag = "ml-IN",
        englishName = "Malayalam",
        nativeName = "മലയാളം",
        scriptName = "Malayalam",
        mixedVariantLabel = "Manglish (Malayalam + English)",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Offline speech recognition is limited on older Android devices; internet improves recognition."
    )

    val MARATHI = SupportedLanguage(
        id = "marathi",
        bcp47Tag = "mr-IN",
        englishName = "Marathi",
        nativeName = "मराठी",
        scriptName = "Devanagari",
        mixedVariantLabel = "Marathi + English",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Shares Devanagari script with Hindi; selecting Marathi directly improves voice accuracy."
    )

    val BENGALI = SupportedLanguage(
        id = "bengali",
        bcp47Tag = "bn-IN",
        englishName = "Bengali",
        nativeName = "বাংলা",
        scriptName = "Bengali",
        mixedVariantLabel = "Benglish (Bengali + English)",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Speech input accuracy depends on device speech service; TTS voice pack may require download."
    )

    val GUJARATI = SupportedLanguage(
        id = "gujarati",
        bcp47Tag = "gu-IN",
        englishName = "Gujarati",
        nativeName = "ગુજરાતી",
        scriptName = "Gujarati",
        mixedVariantLabel = "Gujlish (Gujarati + English)",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Offline voice input may be unavailable on some devices without installing Gujarati speech data."
    )

    val PUNJABI = SupportedLanguage(
        id = "punjabi",
        bcp47Tag = "pa-IN",
        englishName = "Punjabi",
        nativeName = "ਪੰਜਾਬੀ",
        scriptName = "Gurmukhi",
        mixedVariantLabel = "Punjabi + English",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Some Android devices lack preinstalled Punjabi TTS voices; English or Hindi can be used as fallback."
    )

    val ODIA = SupportedLanguage(
        id = "odia",
        bcp47Tag = "or-IN",
        englishName = "Odia",
        nativeName = "ଓଡ଼ିଆ",
        scriptName = "Odia",
        mixedVariantLabel = "Odia + English",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Device speech recognition and TTS support for Odia is limited on many Android phones; text guidance still works."
    )

    val ASSAMESE = SupportedLanguage(
        id = "assamese",
        bcp47Tag = "as-IN",
        englishName = "Assamese",
        nativeName = "অসমীয়া",
        scriptName = "Assamese-Bengali",
        mixedVariantLabel = "Assamese + English",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Many Android devices do not ship an Assamese TTS voice by default; VaaniX shows readable text and offers fallback speech."
    )

    val URDU = SupportedLanguage(
        id = "urdu",
        bcp47Tag = "ur-IN",
        englishName = "Urdu",
        nativeName = "اردو",
        scriptName = "Perso-Arabic",
        mixedVariantLabel = "Urdu + English / Hindustani",
        defaultOfflineExpected = false,
        knownLimitationsNote = "Voice recognition may depend on internet connectivity; spoken Hindi/Urdu phrases are supported."
    )

    val ENGLISH_INDIA = SupportedLanguage(
        id = "english_in",
        bcp47Tag = "en-IN",
        englishName = "English (India)",
        nativeName = "English",
        scriptName = "Latin",
        mixedVariantLabel = "Indian English + Mixed",
        defaultOfflineExpected = true,
        knownLimitationsNote = "Widely supported on Android devices; also serves as a reliable fallback when a regional voice pack is missing."
    )

    val majorIndianLanguages: List<SupportedLanguage> = listOf(
        TELUGU,
        HINDI,
        TAMIL,
        KANNADA,
        MALAYALAM,
        MARATHI,
        BENGALI,
        GUJARATI,
        PUNJABI,
        ODIA,
        ASSAMESE,
        URDU
    )

    val allLanguages: List<SupportedLanguage> = majorIndianLanguages + ENGLISH_INDIA

    fun findByTag(tag: String): SupportedLanguage {
        return allLanguages.firstOrNull { it.bcp47Tag.equals(tag, ignoreCase = true) }
            ?: allLanguages.firstOrNull {
                it.bcp47Tag.substringBefore("-").equals(tag.substringBefore("-"), ignoreCase = true)
            }
            ?: ENGLISH_INDIA
    }
}

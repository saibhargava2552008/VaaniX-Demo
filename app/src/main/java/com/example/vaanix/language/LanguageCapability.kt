package com.example.vaanix.language

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SpeechRecognitionStatus {
    AVAILABLE,
    REQUIRES_INTERNET_OR_PACK,
    UNAVAILABLE_ON_DEVICE,
    NOT_CHECKED
}

enum class TtsAvailabilityStatus {
    AVAILABLE,
    MISSING_VOICE_DATA,
    UNAVAILABLE_ON_DEVICE,
    NOT_CHECKED
}

enum class DetectionSupportLevel {
    SCRIPT_AND_MIXED_PHRASES,
    SCRIPT_DETECTION_ONLY
}

enum class TranslationSupportLevel {
    GUIDED_LOCAL_TEMPLATES_ONLY,
    UNAVAILABLE_IN_ZERO_COST_MODE
}

/**
 * Honest, independently tracked capability profile for a single language.
 * VaaniX never claims full support simply because a language appears in a menu.
 */
data class LanguageCapability(
    val language: SupportedLanguage,
    val speechRecognition: SpeechRecognitionStatus,
    val textToSpeech: TtsAvailabilityStatus,
    val languageDetection: DetectionSupportLevel,
    val translation: TranslationSupportLevel,
    val mixedLanguageSupported: Boolean,
    val offlineAvailable: Boolean,
    val knownLimitations: String,
    val fallbackRecommendation: String
) {
    val isVoiceFullyReady: Boolean
        get() = speechRecognition == SpeechRecognitionStatus.AVAILABLE &&
            textToSpeech == TtsAvailabilityStatus.AVAILABLE

    val honestSummary: String
        get() = buildString {
            val sttLabel = when (speechRecognition) {
                SpeechRecognitionStatus.AVAILABLE -> "Voice input ready"
                SpeechRecognitionStatus.REQUIRES_INTERNET_OR_PACK -> "Voice input needs device pack/internet"
                SpeechRecognitionStatus.UNAVAILABLE_ON_DEVICE -> "Voice input unavailable on this phone"
                SpeechRecognitionStatus.NOT_CHECKED -> "Checking voice input"
            }
            val ttsLabel = when (textToSpeech) {
                TtsAvailabilityStatus.AVAILABLE -> "Voice reading ready"
                TtsAvailabilityStatus.MISSING_VOICE_DATA -> "Voice pack download needed"
                TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE -> "Voice reading unavailable (text shown)"
                TtsAvailabilityStatus.NOT_CHECKED -> "Checking voice reading"
            }
            append("$sttLabel • $ttsLabel")
        }
}

interface DeviceSpeechCapabilityProbe {
    fun isSpeechRecognitionServiceAvailable(): Boolean
    fun checkTtsLanguageStatus(bcp47Tag: String): TtsAvailabilityStatus
}

/**
 * Central registry tracking actual device and provider capabilities per language.
 */
class LanguageCapabilityRegistry(
    initialLanguages: List<SupportedLanguage> = LanguageCatalog.allLanguages
) {
    private val _capabilities = MutableStateFlow(
        initialLanguages.associate { lang ->
            lang.bcp47Tag to buildDefaultCapability(lang)
        }
    )
    val capabilities: StateFlow<Map<String, LanguageCapability>> = _capabilities.asStateFlow()

    fun getCapability(bcp47Tag: String): LanguageCapability {
        val lang = LanguageCatalog.findByTag(bcp47Tag)
        return _capabilities.value[lang.bcp47Tag] ?: buildDefaultCapability(lang)
    }

    fun getAllCapabilities(): List<LanguageCapability> {
        return LanguageCatalog.allLanguages.map { getCapability(it.bcp47Tag) }
    }

    /**
     * Refreshes capabilities from the real Android SpeechRecognizer and TextToSpeech engine.
     */
    fun refreshFromDevice(probe: DeviceSpeechCapabilityProbe) {
        val sttServiceAvailable = probe.isSpeechRecognitionServiceAvailable()
        _capabilities.update { current ->
            current.mapValues { (_, existing) ->
                val lang = existing.language
                val ttsStatus = probe.checkTtsLanguageStatus(lang.bcp47Tag)
                val sttStatus = when {
                    !sttServiceAvailable -> SpeechRecognitionStatus.UNAVAILABLE_ON_DEVICE
                    lang.defaultOfflineExpected -> SpeechRecognitionStatus.AVAILABLE
                    else -> SpeechRecognitionStatus.REQUIRES_INTERNET_OR_PACK
                }
                val fallback = buildFallbackRecommendation(lang, sttStatus, ttsStatus)
                existing.copy(
                    speechRecognition = sttStatus,
                    textToSpeech = ttsStatus,
                    offlineAvailable = lang.defaultOfflineExpected &&
                        ttsStatus == TtsAvailabilityStatus.AVAILABLE,
                    fallbackRecommendation = fallback
                )
            }
        }
    }

    fun updateSingleLanguageProbe(
        bcp47Tag: String,
        sttStatus: SpeechRecognitionStatus,
        ttsStatus: TtsAvailabilityStatus
    ) {
        val lang = LanguageCatalog.findByTag(bcp47Tag)
        _capabilities.update { current ->
            val prev = current[lang.bcp47Tag] ?: buildDefaultCapability(lang)
            current + (lang.bcp47Tag to prev.copy(
                speechRecognition = sttStatus,
                textToSpeech = ttsStatus,
                offlineAvailable = lang.defaultOfflineExpected &&
                    ttsStatus == TtsAvailabilityStatus.AVAILABLE,
                fallbackRecommendation = buildFallbackRecommendation(lang, sttStatus, ttsStatus)
            ))
        }
    }

    private fun buildDefaultCapability(lang: SupportedLanguage): LanguageCapability {
        val detection = if (lang.id in setOf("hindi", "telugu", "tamil", "kannada", "malayalam", "marathi", "urdu", "english_in")) {
            DetectionSupportLevel.SCRIPT_AND_MIXED_PHRASES
        } else {
            DetectionSupportLevel.SCRIPT_DETECTION_ONLY
        }
        return LanguageCapability(
            language = lang,
            speechRecognition = SpeechRecognitionStatus.NOT_CHECKED,
            textToSpeech = TtsAvailabilityStatus.NOT_CHECKED,
            languageDetection = detection,
            translation = TranslationSupportLevel.GUIDED_LOCAL_TEMPLATES_ONLY,
            mixedLanguageSupported = true,
            offlineAvailable = lang.defaultOfflineExpected,
            knownLimitations = lang.knownLimitationsNote,
            fallbackRecommendation = "If ${lang.englishName} voice is unavailable on this phone, you can read large text replies or switch voice to English (India) or Hindi."
        )
    }

    private fun buildFallbackRecommendation(
        lang: SupportedLanguage,
        sttStatus: SpeechRecognitionStatus,
        ttsStatus: TtsAvailabilityStatus
    ): String {
        return when {
            sttStatus == SpeechRecognitionStatus.UNAVAILABLE_ON_DEVICE &&
                ttsStatus == TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE ->
                "Voice input and reading for ${lang.englishName} are not installed on this phone. Use the Type button and read large text replies, or switch to English (India)."

            ttsStatus == TtsAvailabilityStatus.MISSING_VOICE_DATA ->
                "Install the ${lang.englishName} voice pack in Android Settings -> Accessibility -> Text-to-Speech, or use English (India) for spoken audio while keeping ${lang.nativeName} text."

            ttsStatus == TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE ->
                "${lang.englishName} spoken audio is not supported by this phone's voice engine. VaaniX will show clear ${lang.nativeName} text and can speak in English (India) as a fallback."

            sttStatus == SpeechRecognitionStatus.REQUIRES_INTERNET_OR_PACK ->
                "For best ${lang.englishName} voice recognition, keep internet on or download the offline ${lang.englishName} speech pack on your phone."

            else ->
                "${lang.englishName} is ready on this phone. You can also mix simple English words like WhatsApp, YouTube, or Wi-Fi."
        }
    }
}

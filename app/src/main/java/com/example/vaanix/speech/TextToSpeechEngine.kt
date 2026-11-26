package com.example.vaanix.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.vaanix.language.TtsAvailabilityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class SpeechSpeed(val rate: Float, val label: String) {
    SLOW(0.74f, "Slower (Easy to follow)"),
    NORMAL(0.94f, "Normal"),
    BRISK(1.12f, "Faster")
}

sealed class TtsState {
    data object Initializing : TtsState()
    data object Ready : TtsState()
    data class Speaking(val utteranceId: String, val spokenLanguageTag: String) : TtsState()
    data class Error(val message: String) : TtsState()
}

data class SpeakResult(
    val started: Boolean,
    val usedFallbackLocale: Boolean = false,
    val warningMessage: String? = null
)

interface TextToSpeechEngine {
    val state: StateFlow<TtsState>
    val isReady: Boolean
    fun checkLanguageAvailability(bcp47Tag: String): TtsAvailabilityStatus
    fun speak(text: String, bcp47Tag: String, speed: SpeechSpeed): SpeakResult
    fun stop()
    fun shutdown()
}

/**
 * Lifecycle-safe Android TextToSpeech wrapper with honest per-language availability checks,
 * optional slower elderly speech speed, and automatic fallback to English (India) with notice
 * when a regional voice pack is missing on the device.
 */
class AndroidTextToSpeechEngine(
    context: Context,
    private val onInitializedCallback: (() -> Unit)? = null
) : TextToSpeechEngine {

    private val _state = MutableStateFlow<TtsState>(TtsState.Initializing)
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    @Volatile
    private var engineReady: Boolean = false
    override val isReady: Boolean
        get() = engineReady

    private var tts: TextToSpeech? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engineReady = true
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            val current = _state.value
                            val langTag = (current as? TtsState.Speaking)?.spokenLanguageTag ?: "en-IN"
                            _state.value = TtsState.Speaking(utteranceId.orEmpty(), langTag)
                        }

                        override fun onDone(utteranceId: String?) {
                            _state.value = TtsState.Ready
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _state.value = TtsState.Error("Voice playback stopped.")
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _state.value = TtsState.Error("Could not finish reading aloud.")
                        }
                    })
                    _state.value = TtsState.Ready
                    onInitializedCallback?.invoke()
                } else {
                    engineReady = false
                    _state.value = TtsState.Error("Text-to-Speech engine is unavailable on this device.")
                    onInitializedCallback?.invoke()
                }
            }
        } catch (_: Throwable) {
            engineReady = false
            _state.value = TtsState.Error("Unable to start Text-to-Speech on this device.")
        }
    }

    override fun checkLanguageAvailability(bcp47Tag: String): TtsAvailabilityStatus {
        val instance = tts
        if (!engineReady || instance == null) return TtsAvailabilityStatus.NOT_CHECKED
        return try {
            val locale = Locale.forLanguageTag(bcp47Tag)
            when (instance.isLanguageAvailable(locale)) {
                TextToSpeech.LANG_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_AVAILABLE,
                TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> TtsAvailabilityStatus.AVAILABLE
                TextToSpeech.LANG_MISSING_DATA -> TtsAvailabilityStatus.MISSING_VOICE_DATA
                else -> TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE
            }
        } catch (_: Throwable) {
            TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE
        }
    }

    override fun speak(text: String, bcp47Tag: String, speed: SpeechSpeed): SpeakResult {
        val instance = tts
        if (!engineReady || instance == null || text.isBlank()) {
            return SpeakResult(
                started = false,
                warningMessage = "Voice reading is not ready yet; please read the reply on screen."
            )
        }

        val availability = checkLanguageAvailability(bcp47Tag)
        var usedFallback = false
        var warning: String? = null
        val targetTag = when (availability) {
            TtsAvailabilityStatus.AVAILABLE -> bcp47Tag
            TtsAvailabilityStatus.MISSING_VOICE_DATA -> {
                usedFallback = true
                warning = "Voice pack for $bcp47Tag is not downloaded yet. Showing text clearly and trying fallback voice."
                "en-IN"
            }
            else -> {
                usedFallback = true
                warning = "Spoken voice for $bcp47Tag is unavailable on this phone. Reply is displayed clearly on screen."
                "en-IN"
            }
        }

        return try {
            instance.language = Locale.forLanguageTag(targetTag)
            instance.setSpeechRate(speed.rate)
            instance.setPitch(1.0f)
            val utteranceId = UUID.randomUUID().toString()
            _state.value = TtsState.Speaking(utteranceId, targetTag)
            val code = instance.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (code == TextToSpeech.SUCCESS) {
                SpeakResult(started = true, usedFallbackLocale = usedFallback, warningMessage = warning)
            } else {
                _state.value = TtsState.Ready
                SpeakResult(started = false, warningMessage = "Could not speak right now; reply is shown in text.")
            }
        } catch (_: Throwable) {
            _state.value = TtsState.Ready
            SpeakResult(started = false, warningMessage = "Voice playback failed on this device.")
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {
            // Safe stop
        } finally {
            if (engineReady) {
                _state.value = TtsState.Ready
            }
        }
    }

    override fun shutdown() {
        engineReady = false
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
            // Safe teardown
        } finally {
            tts = null
            _state.value = TtsState.Initializing
        }
    }
}

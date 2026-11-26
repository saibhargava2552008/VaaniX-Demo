package com.example.vaanix.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechInputState {
    data object Idle : SpeechInputState()
    data class Listening(val partialText: String = "", val soundLevelDb: Float = 0f) : SpeechInputState()
    data class Result(val transcript: String, val localeTag: String) : SpeechInputState()
    data class Error(
        val errorCode: Int,
        val userFriendlyMessage: String,
        val isPermissionIssue: Boolean = false
    ) : SpeechInputState()
}

interface SpeechInputEngine {
    val state: StateFlow<SpeechInputState>
    fun isAvailableOnDevice(): Boolean
    fun startListening(bcp47Tag: String)
    fun stopListening()
    fun cancel()
    fun clearState()
    fun destroy()
}

/**
 * Lifecycle-safe wrapper around Android's SpeechRecognizer.
 * Enforces main-thread execution, prevents leaked recognizer instances,
 * and maps technical Android SpeechRecognizer error codes into calm,
 * plain-language guidance for elderly users.
 */
class AndroidSpeechInputEngine(
    private val context: Context
) : SpeechInputEngine {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var activeLocaleTag: String = "en-IN"
    private var isListeningSessionActive: Boolean = false

    private val _state = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
    override val state: StateFlow<SpeechInputState> = _state.asStateFlow()

    override fun isAvailableOnDevice(): Boolean {
        return try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (_: Throwable) {
            false
        }
    }

    override fun startListening(bcp47Tag: String) {
        activeLocaleTag = bcp47Tag
        runOnMain {
            if (!isAvailableOnDevice()) {
                _state.value = SpeechInputState.Error(
                    errorCode = SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                    userFriendlyMessage = "Voice recognition is not installed on this phone. You can use the Type button below instead."
                )
                return@runOnMain
            }

            try {
                // Clean up any stale recognizer session before starting a new one
                destroyInternal()
                val instance = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                recognizer = instance
                instance.setRecognitionListener(createListener())

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, bcp47Tag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, bcp47Tag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                }

                isListeningSessionActive = true
                _state.value = SpeechInputState.Listening()
                instance.startListening(intent)
            } catch (_: SecurityException) {
                isListeningSessionActive = false
                _state.value = SpeechInputState.Error(
                    errorCode = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                    userFriendlyMessage = "Please allow microphone access so VaaniX can hear you.",
                    isPermissionIssue = true
                )
            } catch (_: Throwable) {
                isListeningSessionActive = false
                destroyInternal()
                _state.value = SpeechInputState.Error(
                    errorCode = -1,
                    userFriendlyMessage = "Could not start the microphone right now. Please try again or type your request."
                )
            }
        }
    }

    override fun stopListening() {
        runOnMain {
            if (isListeningSessionActive) {
                try {
                    recognizer?.stopListening()
                } catch (_: Throwable) {
                    destroyInternal()
                    _state.value = SpeechInputState.Idle
                }
            }
        }
    }

    override fun cancel() {
        runOnMain {
            isListeningSessionActive = false
            try {
                recognizer?.cancel()
            } catch (_: Throwable) {
                // Ignore recognizer cancel exceptions during teardown
            } finally {
                destroyInternal()
                _state.value = SpeechInputState.Idle
            }
        }
    }

    override fun clearState() {
        if (!isListeningSessionActive) {
            _state.value = SpeechInputState.Idle
        }
    }

    override fun destroy() {
        runOnMain {
            isListeningSessionActive = false
            destroyInternal()
            _state.value = SpeechInputState.Idle
        }
    }

    private fun destroyInternal() {
        try {
            recognizer?.setRecognitionListener(null)
            recognizer?.destroy()
        } catch (_: Throwable) {
            // Safe cleanup
        } finally {
            recognizer = null
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isListeningSessionActive) {
                _state.value = SpeechInputState.Listening()
            }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            val current = _state.value
            if (isListeningSessionActive && current is SpeechInputState.Listening) {
                _state.value = current.copy(soundLevelDb = rmsdB.coerceIn(0f, 12f))
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (!isListeningSessionActive) return
            isListeningSessionActive = false
            destroyInternal()

            // ERROR_CLIENT often fires during intentional cancellation; stay calm and idle
            if (error == SpeechRecognizer.ERROR_CLIENT) {
                _state.value = SpeechInputState.Idle
                return
            }

            _state.value = SpeechInputState.Error(
                errorCode = error,
                userFriendlyMessage = mapErrorToFriendlyMessage(error),
                isPermissionIssue = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
            )
        }

        override fun onResults(results: Bundle?) {
            isListeningSessionActive = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            destroyInternal()
            if (best.isNotEmpty()) {
                _state.value = SpeechInputState.Result(best, activeLocaleTag)
            } else {
                _state.value = SpeechInputState.Error(
                    errorCode = SpeechRecognizer.ERROR_NO_MATCH,
                    userFriendlyMessage = "I did not catch that clearly. Please tap the microphone and speak a little slower."
                )
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isListeningSessionActive) return
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            val current = _state.value
            if (partial.isNotEmpty() && current is SpeechInputState.Listening) {
                _state.value = current.copy(partialText = partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    companion object {
        fun mapErrorToFriendlyMessage(errorCode: Int): String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO ->
                "There was a problem reading from the microphone. Please check if another app is using it."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Please allow microphone permission so VaaniX can listen when you tap Speak."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Voice recognition for this language needs an internet connection right now. You can also type below."
            SpeechRecognizer.ERROR_NO_MATCH ->
                "I could not understand what was said. Please tap Speak and try again slowly."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "I did not hear any voice. Please tap Speak and talk close to the phone."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "The voice listener is busy. Please wait a moment and try again."
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                "This phone does not have voice input installed for this language yet. You can type or switch language."
            else ->
                "Voice listening stopped unexpectedly. Please tap Speak to try again."
        }
    }
}

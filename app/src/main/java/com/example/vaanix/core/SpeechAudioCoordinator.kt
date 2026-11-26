package com.example.vaanix.core

import com.example.vaanix.language.DeviceSpeechCapabilityProbe
import com.example.vaanix.language.TtsAvailabilityStatus
import com.example.vaanix.speech.SpeakResult
import com.example.vaanix.speech.SpeechInputEngine
import com.example.vaanix.speech.SpeechInputState
import com.example.vaanix.speech.SpeechSpeed
import com.example.vaanix.speech.TextToSpeechEngine
import com.example.vaanix.speech.TtsState
import kotlinx.coroutines.flow.StateFlow

/**
 * Prevents microphone and speaker conflicts by ensuring:
 * 1. Text-to-Speech always stops before microphone listening starts.
 * 2. Microphone listening is always cancelled before Text-to-Speech speaks.
 * 3. Cancel/Stop and lifecycle pause cleanly release both audio resources.
 */
class SpeechAudioCoordinator(
    private val speechInput: SpeechInputEngine,
    private val ttsEngine: TextToSpeechEngine
) : DeviceSpeechCapabilityProbe {

    val speechInputState: StateFlow<SpeechInputState> = speechInput.state
    val ttsState: StateFlow<TtsState> = ttsEngine.state

    fun startListening(bcp47Tag: String) {
        // Always silence TTS first so the microphone never hears the assistant's own voice
        ttsEngine.stop()
        speechInput.startListening(bcp47Tag)
    }

    fun stopListening() {
        speechInput.stopListening()
    }

    fun speak(text: String, bcp47Tag: String, speed: SpeechSpeed): SpeakResult {
        // Cancel active mic capture before speaking aloud
        if (speechInput.state.value is SpeechInputState.Listening) {
            speechInput.cancel()
        }
        return ttsEngine.speak(text, bcp47Tag, speed)
    }

    fun stopAllAudio() {
        speechInput.cancel()
        ttsEngine.stop()
    }

    fun clearSpeechError() {
        speechInput.clearState()
    }

    fun onAppBackgrounded() {
        // Privacy & lifecycle rule: never record audio in the background
        speechInput.cancel()
        ttsEngine.stop()
    }

    fun release() {
        speechInput.destroy()
        ttsEngine.shutdown()
    }

    override fun isSpeechRecognitionServiceAvailable(): Boolean {
        return speechInput.isAvailableOnDevice()
    }

    override fun checkTtsLanguageStatus(bcp47Tag: String): TtsAvailabilityStatus {
        return ttsEngine.checkLanguageAvailability(bcp47Tag)
    }
}

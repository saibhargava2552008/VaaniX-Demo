package com.example.vaanix

import com.example.vaanix.ai.AssistantIntent
import com.example.vaanix.ai.ConversationContext
import com.example.vaanix.ai.MockMultilingualAssistantProvider
import com.example.vaanix.core.VaaniConfig
import com.example.vaanix.language.DeviceSpeechCapabilityProbe
import com.example.vaanix.language.LanguageCapabilityRegistry
import com.example.vaanix.language.LanguageCatalog
import com.example.vaanix.language.LanguageDetector
import com.example.vaanix.language.LocalizedStringsProvider
import com.example.vaanix.language.SpeechRecognitionStatus
import com.example.vaanix.language.TtsAvailabilityStatus
import com.example.vaanix.screenintelligence.ScreenContentAnalyzer
import com.example.vaanix.screenintelligence.ScreenSafetyAlert
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PanIndiaMultilingualTest {

    private val provider = MockMultilingualAssistantProvider()

    @Test
    fun `zero cost configuration is preserved`() {
        assertEquals("mock", VaaniConfig.llmProvider)
        assertFalse(VaaniConfig.allowRealApiCalls)
        assertTrue(VaaniConfig.isMockProviderActive)
    }

    @Test
    fun `all 12 major Indian languages are registered with native copy and capabilities`() {
        assertEquals(12, LanguageCatalog.majorIndianLanguages.size)
        val registry = LanguageCapabilityRegistry()
        for (lang in LanguageCatalog.majorIndianLanguages) {
            val copy = LocalizedStringsProvider.forLanguage(lang)
            assertTrue("Missing greeting for ${lang.englishName}", copy.greeting.isNotBlank())
            assertTrue("Missing sample prompts for ${lang.englishName}", copy.samplePrompts.isNotEmpty())
            val cap = registry.getCapability(lang.bcp47Tag)
            assertEquals(lang.bcp47Tag, cap.language.bcp47Tag)
            assertTrue(cap.knownLimitations.isNotBlank())
        }
    }

    @Test
    fun `all 12 Indian language native speech inputs are understood and answered in selected language`() = runTest {
        val cases = listOf(
            LanguageCatalog.TELUGU to "WhatsApp తెరువు",
            LanguageCatalog.HINDI to "WhatsApp खोलो",
            LanguageCatalog.TAMIL to "WhatsApp திறக்கவும்",
            LanguageCatalog.KANNADA to "WhatsApp ತೆರೆಯಿರಿ",
            LanguageCatalog.MALAYALAM to "WhatsApp തുറക്കൂ",
            LanguageCatalog.MARATHI to "WhatsApp उघडा",
            LanguageCatalog.BENGALI to "WhatsApp খুলুন",
            LanguageCatalog.GUJARATI to "WhatsApp ખોલો",
            LanguageCatalog.PUNJABI to "WhatsApp ਖੋਲ੍ਹੋ",
            LanguageCatalog.ODIA to "WhatsApp ଖୋଲନ୍ତୁ",
            LanguageCatalog.ASSAMESE to "WhatsApp খোলক",
            LanguageCatalog.URDU to "WhatsApp کھولیں"
        )

        for ((language, utterance) in cases) {
            val detected = LanguageDetector.detect(utterance, language)
            assertEquals("Script detection failed for ${language.englishName}", language.bcp47Tag, detected.detectedLanguage.bcp47Tag)

            val reply = provider.processRequest(
                userText = utterance,
                context = ConversationContext(preferredLanguage = language)
            )
            assertTrue("Expected OpenApp intent for ${language.englishName}", reply.intent is AssistantIntent.OpenApp)
            assertEquals(language.bcp47Tag, reply.responseLanguage.bcp47Tag)
            assertTrue(reply.isMockProvider)
            assertNotNull(reply.proposedAction)
        }
    }

    @Test
    fun `mixed language Hinglish and Tenglish speech detection and intent matching`() = runTest {
        // Hinglish YouTube search
        val hinglish = "YouTube open karke songs search karo"
        val detHi = LanguageDetector.detect(hinglish, LanguageCatalog.ENGLISH_INDIA)
        assertEquals(LanguageCatalog.HINDI.bcp47Tag, detHi.detectedLanguage.bcp47Tag)
        assertTrue(detHi.isMixedLanguage)

        val replyHi = provider.processRequest(hinglish, ConversationContext(preferredLanguage = LanguageCatalog.HINDI))
        assertTrue(replyHi.intent is AssistantIntent.SearchYouTube)

        // Tenglish sensitive WhatsApp message request
        val tenglish = "Naa kodukuki message pampinchu"
        val detTe = LanguageDetector.detect(tenglish, LanguageCatalog.ENGLISH_INDIA)
        assertEquals(LanguageCatalog.TELUGU.bcp47Tag, detTe.detectedLanguage.bcp47Tag)
        assertTrue(detTe.isMixedLanguage)

        val replyTe = provider.processRequest(tenglish, ConversationContext(preferredLanguage = LanguageCatalog.TELUGU))
        assertTrue(replyTe.intent is AssistantIntent.PrepareWhatsAppMessage)
        assertTrue("Sending a message must require confirmation", replyTe.proposedAction?.requiresConfirmation == true)

        // Hinglish incoming message privacy check
        val msgCheck = "Mere phone mein kya message aaya hai?"
        val replyCheck = provider.processRequest(msgCheck, ConversationContext(preferredLanguage = LanguageCatalog.HINDI))
        assertTrue(replyCheck.intent is AssistantIntent.CheckMessagesHelp)

        // Hinglish teach phone
        val teach = "Mujhe phone use karna sikhao"
        val replyTeach = provider.processRequest(teach, ConversationContext(preferredLanguage = LanguageCatalog.HINDI))
        assertTrue(replyTeach.intent is AssistantIntent.TeachPhone)
    }

    @Test
    fun `follow up conversation context works for YouTube search`() = runTest {
        val turn1 = provider.processRequest(
            userText = "Open YouTube",
            context = ConversationContext(preferredLanguage = LanguageCatalog.ENGLISH_INDIA)
        )
        assertTrue(turn1.intent is AssistantIntent.OpenApp)
        assertEquals("youtube", turn1.updatedLastAppKey)

        val turn2 = provider.processRequest(
            userText = "Search devotional songs",
            context = ConversationContext(
                preferredLanguage = LanguageCatalog.ENGLISH_INDIA,
                lastOpenedAppKey = turn1.updatedLastAppKey
            )
        )
        assertTrue(turn2.intent is AssistantIntent.SearchYouTube)
        val searchIntent = turn2.intent as AssistantIntent.SearchYouTube
        assertTrue(searchIntent.query.contains("devotional songs", ignoreCase = true))
    }

    @Test
    fun `capability registry honestly reports missing regional TTS packs and unavailable STT`() {
        val registry = LanguageCapabilityRegistry()
        val fakeProbe = object : DeviceSpeechCapabilityProbe {
            override fun isSpeechRecognitionServiceAvailable(): Boolean = false
            override fun checkTtsLanguageStatus(bcp47Tag: String): TtsAvailabilityStatus {
                return when (bcp47Tag) {
                    "en-IN", "hi-IN" -> TtsAvailabilityStatus.AVAILABLE
                    "or-IN" -> TtsAvailabilityStatus.MISSING_VOICE_DATA
                    else -> TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE
                }
            }
        }

        registry.refreshFromDevice(fakeProbe)

        val odiaCap = registry.getCapability("or-IN")
        assertEquals(SpeechRecognitionStatus.UNAVAILABLE_ON_DEVICE, odiaCap.speechRecognition)
        assertEquals(TtsAvailabilityStatus.MISSING_VOICE_DATA, odiaCap.textToSpeech)
        assertFalse(odiaCap.isVoiceFullyReady)
        assertTrue(odiaCap.fallbackRecommendation.contains("voice pack", ignoreCase = true))

        val assameseCap = registry.getCapability("as-IN")
        assertEquals(TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE, assameseCap.textToSpeech)
        assertFalse(assameseCap.offlineAvailable)
    }

    @Test
    fun `screen content analyzer flags OTP scam risk and handles empty input safely`() {
        val otpResult = ScreenContentAnalyzer.analyzeUserProvidedText(
            "Your bank OTP is 938472. Valid for 5 minutes.",
            LanguageCatalog.HINDI
        )
        assertEquals(ScreenSafetyAlert.OTP_OR_SCAM_CAUTION, otpResult.safetyAlert)
        assertTrue(otpResult.simpleExplanation.contains("OTP"))

        val emptyResult = ScreenContentAnalyzer.analyzeUserProvidedText("   ", LanguageCatalog.TELUGU)
        assertEquals(ScreenSafetyAlert.NONE, emptyResult.safetyAlert)
        assertTrue(emptyResult.simpleExplanation.contains("never in the background", ignoreCase = true))
    }

    @Test
    fun `speech error codes map to elderly friendly plain language`() {
        val permMsg = com.example.vaanix.speech.AndroidSpeechInputEngine.mapErrorToFriendlyMessage(
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
        )
        val noMatchMsg = com.example.vaanix.speech.AndroidSpeechInputEngine.mapErrorToFriendlyMessage(
            android.speech.SpeechRecognizer.ERROR_NO_MATCH
        )
        val networkMsg = com.example.vaanix.speech.AndroidSpeechInputEngine.mapErrorToFriendlyMessage(
            android.speech.SpeechRecognizer.ERROR_NETWORK
        )
        assertTrue(permMsg.contains("microphone permission", ignoreCase = true))
        assertTrue(noMatchMsg.contains("try again", ignoreCase = true))
        assertTrue(networkMsg.contains("internet", ignoreCase = true))
    }
}

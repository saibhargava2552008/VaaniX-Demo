package com.example.vaanix

import com.example.vaanix.ai.MockMultilingualAssistantProvider
import com.example.vaanix.core.SpeechAudioCoordinator
import com.example.vaanix.data.InMemoryConversationRepository
import com.example.vaanix.data.InMemoryPreferencesStore
import com.example.vaanix.language.LanguageCapabilityRegistry
import com.example.vaanix.language.LanguageCatalog
import com.example.vaanix.language.TtsAvailabilityStatus
import com.example.vaanix.phoneactions.ActionExecutionResult
import com.example.vaanix.phoneactions.PhoneAction
import com.example.vaanix.phoneactions.PhoneActionExecutor
import com.example.vaanix.speech.SpeakResult
import com.example.vaanix.speech.SpeechInputEngine
import com.example.vaanix.speech.SpeechInputState
import com.example.vaanix.speech.SpeechSpeed
import com.example.vaanix.speech.TextToSpeechEngine
import com.example.vaanix.speech.TtsState
import com.example.vaanix.ui.VaaniXViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VaaniXViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeSpeechInput: FakeSpeechInputEngine
    private lateinit var fakeTts: FakeTextToSpeechEngine
    private lateinit var fakeExecutor: FakePhoneActionExecutor
    private lateinit var repo: InMemoryConversationRepository
    private lateinit var prefs: InMemoryPreferencesStore
    private lateinit var registry: LanguageCapabilityRegistry
    private lateinit var viewModel: VaaniXViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeSpeechInput = FakeSpeechInputEngine()
        fakeTts = FakeTextToSpeechEngine()
        fakeExecutor = FakePhoneActionExecutor()
        repo = InMemoryConversationRepository()
        prefs = InMemoryPreferencesStore()
        registry = LanguageCapabilityRegistry()

        viewModel = VaaniXViewModel(
            audioCoordinator = SpeechAudioCoordinator(fakeSpeechInput, fakeTts),
            languageRegistry = registry,
            assistantProvider = MockMultilingualAssistantProvider(),
            actionExecutor = fakeExecutor,
            conversationRepository = repo,
            preferencesStore = prefs
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `microphone permission denial shows rationale and reveals typing fallback`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onMicPermissionDenied()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.micPermissionGranted)
        assertTrue(state.showMicPermissionRationale)
        assertTrue(state.showTextInputBox)
        collector.cancel()
    }

    @Test
    fun `starting mic stops active TTS and stop button cancels recording cleanly`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateMicPermissionStatus(true)
        viewModel.speakMessage("Hello from VaaniX")
        assertTrue(fakeTts.isSpeakingNow)

        viewModel.onMicrophoneButtonPressed()
        advanceUntilIdle()
        assertFalse("TTS must stop before mic starts", fakeTts.isSpeakingNow)
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.onStopAllPressed()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isListening)
        assertTrue(fakeSpeechInput.cancelCount >= 1)
        collector.cancel()
    }

    @Test
    fun `background lifecycle stops mic and speaker immediately`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.updateMicPermissionStatus(true)
        viewModel.onMicrophoneButtonPressed()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isListening)

        viewModel.onAppBackgrounded()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isListening)
        assertFalse(fakeTts.isSpeakingNow)
        collector.cancel()
    }

    @Test
    fun `sensitive action requires explicit confirmation before execution and supports cancel`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // Request WhatsApp message (sensitive)
        viewModel.submitUserUtterance("Naa kodukuki message pampinchu")
        advanceUntilIdle()

        assertTrue("Sensitive action must NOT execute before confirmation", fakeExecutor.executedActions.isEmpty())
        assertNotNull(viewModel.uiState.value.pendingConfirmation)

        // User cancels
        viewModel.cancelPendingAction()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pendingConfirmation)
        assertTrue(fakeExecutor.executedActions.isEmpty())

        // User asks to make a call and confirms
        viewModel.submitUserUtterance("Call 9876543210")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.pendingConfirmation)

        viewModel.confirmPendingAction()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pendingConfirmation)
        assertEquals(1, fakeExecutor.executedActions.size)
        assertTrue(fakeExecutor.executedActions.first() is PhoneAction.PreparePhoneCall)
        collector.cancel()
    }

    @Test
    fun `language switching persists preference and surfaces honest warning for unavailable TTS`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        fakeTts.unavailableLocales.add("as-IN")
        viewModel.refreshLanguageCapabilities()

        viewModel.onLanguageSelected(LanguageCatalog.ASSAMESE)
        advanceUntilIdle()

        assertEquals("as-IN", viewModel.uiState.value.selectedLanguage.bcp47Tag)
        assertNotNull(
            "Should warn honestly when regional voice is unavailable",
            viewModel.uiState.value.voiceWarningBanner
        )
        collector.cancel()
    }

    @Test
    fun `speech recognition failure surfaces friendly error state without crashing`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        fakeSpeechInput.emitError(7, "I could not understand what was said. Please tap Speak and try again slowly.")
        advanceUntilIdle()

        val speechState = viewModel.uiState.value.speechInputState
        assertTrue(speechState is SpeechInputState.Error)
        assertTrue((speechState as SpeechInputState.Error).userFriendlyMessage.contains("try again", ignoreCase = true))
        collector.cancel()
    }

    @Test
    fun `follow up YouTube search executes safely after Open YouTube`() = runTest {
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.submitUserUtterance("Open YouTube")
        advanceUntilIdle()
        assertEquals("youtube", viewModel.uiState.value.lastOpenedAppKey)

        viewModel.submitUserUtterance("Search devotional songs")
        advanceUntilIdle()
        assertTrue(fakeExecutor.executedActions.any { it is PhoneAction.SearchYouTube })
        collector.cancel()
    }
}

private class FakeSpeechInputEngine : SpeechInputEngine {
    private val _state = MutableStateFlow<SpeechInputState>(SpeechInputState.Idle)
    override val state: StateFlow<SpeechInputState> = _state.asStateFlow()
    var cancelCount = 0

    override fun isAvailableOnDevice(): Boolean = true

    override fun startListening(bcp47Tag: String) {
        _state.value = SpeechInputState.Listening()
    }

    override fun stopListening() {
        _state.value = SpeechInputState.Idle
    }

    override fun cancel() {
        cancelCount++
        _state.value = SpeechInputState.Idle
    }

    override fun clearState() {
        _state.value = SpeechInputState.Idle
    }

    override fun destroy() {
        _state.value = SpeechInputState.Idle
    }

    fun emitError(code: Int, message: String) {
        _state.value = SpeechInputState.Error(code, message)
    }
}

private class FakeTextToSpeechEngine : TextToSpeechEngine {
    private val _state = MutableStateFlow<TtsState>(TtsState.Ready)
    override val state: StateFlow<TtsState> = _state.asStateFlow()
    override val isReady: Boolean = true
    var isSpeakingNow = false
    val unavailableLocales = mutableSetOf<String>()

    override fun checkLanguageAvailability(bcp47Tag: String): TtsAvailabilityStatus {
        return if (bcp47Tag in unavailableLocales) {
            TtsAvailabilityStatus.UNAVAILABLE_ON_DEVICE
        } else {
            TtsAvailabilityStatus.AVAILABLE
        }
    }

    override fun speak(text: String, bcp47Tag: String, speed: SpeechSpeed): SpeakResult {
        isSpeakingNow = true
        _state.value = TtsState.Speaking("test-utt", bcp47Tag)
        val unavailable = bcp47Tag in unavailableLocales
        return SpeakResult(
            started = true,
            usedFallbackLocale = unavailable,
            warningMessage = if (unavailable) "Spoken voice unavailable for $bcp47Tag" else null
        )
    }

    override fun stop() {
        isSpeakingNow = false
        _state.value = TtsState.Ready
    }

    override fun shutdown() {
        isSpeakingNow = false
        _state.value = TtsState.Initializing
    }
}

private class FakePhoneActionExecutor : PhoneActionExecutor {
    val executedActions = mutableListOf<PhoneAction>()

    override fun execute(action: PhoneAction): ActionExecutionResult {
        executedActions.add(action)
        return ActionExecutionResult.Executed("Executed ${action.label}")
    }
}

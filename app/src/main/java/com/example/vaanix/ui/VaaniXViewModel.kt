package com.example.vaanix.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vaanix.ai.AssistantProvider
import com.example.vaanix.ai.ConversationContext
import com.example.vaanix.ai.MockMultilingualAssistantProvider
import com.example.vaanix.core.SpeechAudioCoordinator
import com.example.vaanix.core.VaaniConfig
import com.example.vaanix.data.ConversationMessageEntity
import com.example.vaanix.data.ConversationRepository
import com.example.vaanix.data.DataStorePreferencesRepository
import com.example.vaanix.data.PreferencesStore
import com.example.vaanix.data.RoomConversationRepository
import com.example.vaanix.data.UserPreferences
import com.example.vaanix.data.VaaniDatabase
import com.example.vaanix.language.LanguageCapability
import com.example.vaanix.language.LanguageCapabilityRegistry
import com.example.vaanix.language.LanguageCatalog
import com.example.vaanix.language.LocalizedStringsProvider
import com.example.vaanix.language.SupportedLanguage
import com.example.vaanix.phoneactions.ActionExecutionResult
import com.example.vaanix.phoneactions.AndroidPhoneActionExecutor
import com.example.vaanix.phoneactions.PhoneAction
import com.example.vaanix.phoneactions.PhoneActionExecutor
import com.example.vaanix.screenintelligence.ScreenContentAnalyzer
import com.example.vaanix.screenintelligence.ScreenExplanation
import com.example.vaanix.speech.AndroidSpeechInputEngine
import com.example.vaanix.speech.AndroidTextToSpeechEngine
import com.example.vaanix.speech.SpeechInputState
import com.example.vaanix.speech.SpeechSpeed
import com.example.vaanix.speech.TtsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VaaniTab {
    ASSISTANT,
    SCREEN_AND_PHONE,
    SETTINGS
}

data class PendingActionConfirmation(
    val action: PhoneAction,
    val promptMessage: String
)

data class VaaniUiState(
    val activeTab: VaaniTab = VaaniTab.ASSISTANT,
    val preferences: UserPreferences = UserPreferences(),
    val selectedLanguage: SupportedLanguage = LanguageCatalog.ENGLISH_INDIA,
    val languageCapabilities: Map<String, LanguageCapability> = emptyMap(),
    val messages: List<ConversationMessageEntity> = emptyList(),
    val speechInputState: SpeechInputState = SpeechInputState.Idle,
    val ttsState: TtsState = TtsState.Initializing,
    val isProcessing: Boolean = false,
    val micPermissionGranted: Boolean = false,
    val showMicPermissionRationale: Boolean = false,
    val showLanguagePickerSheet: Boolean = false,
    val showTextInputBox: Boolean = false,
    val pendingConfirmation: PendingActionConfirmation? = null,
    val voiceWarningBanner: String? = null,
    val screenInputText: String = "",
    val screenExplanation: ScreenExplanation? = null,
    val lastOpenedAppKey: String? = null
) {
    val isListening: Boolean
        get() = speechInputState is SpeechInputState.Listening

    val isSpeaking: Boolean
        get() = ttsState is TtsState.Speaking

    val isAudioActive: Boolean
        get() = isListening || isSpeaking

    val currentCapability: LanguageCapability?
        get() = languageCapabilities[selectedLanguage.bcp47Tag]
}

private data class TransientUiState(
    val activeTab: VaaniTab = VaaniTab.ASSISTANT,
    val isProcessing: Boolean = false,
    val micPermissionGranted: Boolean = false,
    val showMicPermissionRationale: Boolean = false,
    val showLanguagePickerSheet: Boolean = false,
    val showTextInputBox: Boolean = false,
    val pendingConfirmation: PendingActionConfirmation? = null,
    val voiceWarningBanner: String? = null,
    val screenInputText: String = "",
    val screenExplanation: ScreenExplanation? = null,
    val lastOpenedAppKey: String? = null
)

class VaaniXViewModel(
    private val audioCoordinator: SpeechAudioCoordinator,
    private val languageRegistry: LanguageCapabilityRegistry,
    private val assistantProvider: AssistantProvider,
    private val actionExecutor: PhoneActionExecutor,
    private val conversationRepository: ConversationRepository,
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientUiState())
    private val recentUtterances = ArrayDeque<String>()

    val uiState: StateFlow<VaaniUiState> = combine(
        preferencesStore.preferences,
        languageRegistry.capabilities,
        conversationRepository.messages,
        audioCoordinator.speechInputState,
        audioCoordinator.ttsState,
        transientState
    ) { values ->
        val prefs = values[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val caps = values[1] as Map<String, LanguageCapability>
        @Suppress("UNCHECKED_CAST")
        val msgs = values[2] as List<ConversationMessageEntity>
        val speechState = values[3] as SpeechInputState
        val ttsState = values[4] as TtsState
        val transient = values[5] as TransientUiState
        val selectedLang = LanguageCatalog.findByTag(prefs.selectedLanguageTag)

        VaaniUiState(
            activeTab = transient.activeTab,
            preferences = prefs,
            selectedLanguage = selectedLang,
            languageCapabilities = caps,
            messages = msgs,
            speechInputState = speechState,
            ttsState = ttsState,
            isProcessing = transient.isProcessing,
            micPermissionGranted = transient.micPermissionGranted,
            showMicPermissionRationale = transient.showMicPermissionRationale,
            showLanguagePickerSheet = transient.showLanguagePickerSheet,
            showTextInputBox = transient.showTextInputBox,
            pendingConfirmation = transient.pendingConfirmation,
            voiceWarningBanner = transient.voiceWarningBanner,
            screenInputText = transient.screenInputText,
            screenExplanation = transient.screenExplanation,
            lastOpenedAppKey = transient.lastOpenedAppKey
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VaaniUiState(
            languageCapabilities = languageRegistry.capabilities.value
        )
    )

    init {
        refreshLanguageCapabilities()
        observeSpeechResults()
    }

    private fun observeSpeechResults() {
        viewModelScope.launch {
            audioCoordinator.speechInputState.collect { speechState ->
                if (speechState is SpeechInputState.Result) {
                    val transcript = speechState.transcript
                    audioCoordinator.clearSpeechError()
                    if (transcript.isNotBlank()) {
                        submitUserUtterance(transcript)
                    }
                }
            }
        }
    }

    fun refreshLanguageCapabilities() {
        languageRegistry.refreshFromDevice(audioCoordinator)
    }

    fun selectTab(tab: VaaniTab) {
        transientState.update { it.copy(activeTab = tab) }
    }

    fun toggleLanguagePicker(show: Boolean) {
        transientState.update { it.copy(showLanguagePickerSheet = show) }
    }

    fun toggleTextInputBox() {
        transientState.update { it.copy(showTextInputBox = !it.showTextInputBox) }
    }

    fun dismissVoiceWarning() {
        transientState.update { it.copy(voiceWarningBanner = null) }
    }

    fun updateMicPermissionStatus(granted: Boolean) {
        transientState.update {
            it.copy(
                micPermissionGranted = granted,
                showMicPermissionRationale = if (granted) false else it.showMicPermissionRationale
            )
        }
    }

    fun onMicPermissionDenied() {
        audioCoordinator.stopAllAudio()
        transientState.update {
            it.copy(
                micPermissionGranted = false,
                showMicPermissionRationale = true,
                showTextInputBox = true
            )
        }
    }

    fun onMicrophoneButtonPressed() {
        val isListeningNow = audioCoordinator.speechInputState.value is SpeechInputState.Listening
        if (isListeningNow) {
            audioCoordinator.stopListening()
            return
        }
        if (!transientState.value.micPermissionGranted) {
            transientState.update { it.copy(showMicPermissionRationale = true) }
            return
        }
        transientState.update { it.copy(voiceWarningBanner = null, showMicPermissionRationale = false) }
        audioCoordinator.startListening(uiState.value.selectedLanguage.bcp47Tag)
    }

    fun onStopAllPressed() {
        audioCoordinator.stopAllAudio()
        transientState.update { it.copy(isProcessing = false) }
    }

    fun onLanguageSelected(language: SupportedLanguage) {
        audioCoordinator.stopAllAudio()
        viewModelScope.launch {
            preferencesStore.setLanguageTag(language.bcp47Tag)
            val cap = languageRegistry.getCapability(language.bcp47Tag)
            val warning = if (!cap.isVoiceFullyReady) cap.fallbackRecommendation else null
            transientState.update {
                it.copy(
                    showLanguagePickerSheet = false,
                    voiceWarningBanner = warning
                )
            }
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setAutoSpeak(enabled)
            if (!enabled) audioCoordinator.stopAllAudio()
        }
    }

    fun setSpeechSpeed(speed: SpeechSpeed) {
        viewModelScope.launch { preferencesStore.setSpeechSpeed(speed) }
    }

    fun setExtraLargeText(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setExtraLargeText(enabled) }
    }

    fun setHighContrastMode(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setHighContrastMode(enabled) }
    }

    fun submitUserUtterance(rawText: String) {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) return

        audioCoordinator.stopAllAudio()
        val snapshot = uiState.value
        val activeLang = snapshot.selectedLanguage

        viewModelScope.launch {
            transientState.update {
                it.copy(
                    isProcessing = true,
                    pendingConfirmation = null,
                    voiceWarningBanner = null
                )
            }

            // Keep bounded conversation context
            recentUtterances.addLast(cleaned)
            while (recentUtterances.size > VaaniConfig.MAX_CONVERSATION_CONTEXT_TURNS) {
                recentUtterances.removeFirst()
            }

            conversationRepository.addMessage(
                ConversationMessageEntity(
                    isFromUser = true,
                    text = cleaned,
                    languageTag = activeLang.bcp47Tag
                )
            )

            val reply = assistantProvider.processRequest(
                userText = cleaned,
                context = ConversationContext(
                    preferredLanguage = activeLang,
                    lastOpenedAppKey = transientState.value.lastOpenedAppKey,
                    recentUserUtterances = recentUtterances.toList()
                )
            )

            val proposed = reply.proposedAction
            var actionNote: String? = null
            var newPending: PendingActionConfirmation? = null

            if (proposed != null) {
                if (proposed.requiresConfirmation) {
                    val localized = LocalizedStringsProvider.forLanguage(activeLang)
                    newPending = PendingActionConfirmation(
                        action = proposed,
                        promptMessage = "${localized.askConfirmationPrefix}${proposed.label}"
                    )
                    actionNote = "Waiting for your confirmation before continuing."
                } else {
                    val result = actionExecutor.execute(proposed)
                    actionNote = formatActionResult(result)
                }
            }

            val fullAssistantText = if (actionNote != null && proposed != null && !proposed.requiresConfirmation) {
                "${reply.responseText}\n$actionNote"
            } else {
                reply.responseText
            }

            conversationRepository.addMessage(
                ConversationMessageEntity(
                    isFromUser = false,
                    text = fullAssistantText,
                    languageTag = reply.responseLanguage.bcp47Tag,
                    actionStatusNote = actionNote,
                    isMockResponse = reply.isMockProvider
                )
            )

            transientState.update {
                it.copy(
                    isProcessing = false,
                    pendingConfirmation = newPending,
                    lastOpenedAppKey = reply.updatedLastAppKey ?: it.lastOpenedAppKey
                )
            }

            if (uiState.value.preferences.autoSpeakResponses) {
                speakMessage(reply.responseText, reply.responseLanguage.bcp47Tag)
            }
        }
    }

    fun confirmPendingAction() {
        val pending = transientState.value.pendingConfirmation ?: return
        val lang = uiState.value.selectedLanguage
        transientState.update { it.copy(pendingConfirmation = null) }

        viewModelScope.launch {
            val result = actionExecutor.execute(pending.action)
            val statusText = formatActionResult(result)
            conversationRepository.addMessage(
                ConversationMessageEntity(
                    isFromUser = false,
                    text = statusText,
                    languageTag = lang.bcp47Tag,
                    actionStatusNote = "Confirmed by user",
                    isMockResponse = VaaniConfig.isMockProviderActive
                )
            )
            if (uiState.value.preferences.autoSpeakResponses) {
                speakMessage(statusText, lang.bcp47Tag)
            }
        }
    }

    fun cancelPendingAction() {
        val pending = transientState.value.pendingConfirmation ?: return
        val lang = uiState.value.selectedLanguage
        val localized = LocalizedStringsProvider.forLanguage(lang)
        transientState.update { it.copy(pendingConfirmation = null) }

        viewModelScope.launch {
            conversationRepository.addMessage(
                ConversationMessageEntity(
                    isFromUser = false,
                    text = localized.cancelledMessage,
                    languageTag = lang.bcp47Tag,
                    actionStatusNote = "Cancelled: ${pending.action.label}",
                    isMockResponse = VaaniConfig.isMockProviderActive
                )
            )
            if (uiState.value.preferences.autoSpeakResponses) {
                speakMessage(localized.cancelledMessage, lang.bcp47Tag)
            }
        }
    }

    fun triggerQuickPhoneAction(action: PhoneAction) {
        val lang = uiState.value.selectedLanguage
        if (action.requiresConfirmation) {
            val localized = LocalizedStringsProvider.forLanguage(lang)
            transientState.update {
                it.copy(
                    activeTab = VaaniTab.ASSISTANT,
                    pendingConfirmation = PendingActionConfirmation(
                        action = action,
                        promptMessage = "${localized.askConfirmationPrefix}${action.label}"
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            val result = actionExecutor.execute(action)
            val statusText = formatActionResult(result)
            conversationRepository.addMessage(
                ConversationMessageEntity(
                    isFromUser = false,
                    text = statusText,
                    languageTag = lang.bcp47Tag,
                    actionStatusNote = action.label,
                    isMockResponse = VaaniConfig.isMockProviderActive
                )
            )
            if (uiState.value.preferences.autoSpeakResponses) {
                speakMessage(statusText, lang.bcp47Tag)
            }
        }
    }

    fun updateScreenInputText(text: String) {
        transientState.update { it.copy(screenInputText = text) }
    }

    fun handleIncomingSharedText(sharedText: String) {
        val trimmed = sharedText.trim()
        if (trimmed.isEmpty()) return
        transientState.update {
            it.copy(
                activeTab = VaaniTab.SCREEN_AND_PHONE,
                screenInputText = trimmed
            )
        }
        analyzeScreenText(trimmed)
    }

    fun analyzeScreenText(textOverride: String? = null) {
        val textToAnalyze = (textOverride ?: transientState.value.screenInputText).trim()
        val lang = uiState.value.selectedLanguage
        val explanation = ScreenContentAnalyzer.analyzeUserProvidedText(textToAnalyze, lang)
        transientState.update {
            it.copy(
                screenInputText = textToAnalyze,
                screenExplanation = explanation
            )
        }
        if (uiState.value.preferences.autoSpeakResponses) {
            speakMessage("${explanation.title}. ${explanation.simpleExplanation}", lang.bcp47Tag)
        }
    }

    fun speakMessage(text: String, languageTag: String = uiState.value.selectedLanguage.bcp47Tag) {
        val speed = uiState.value.preferences.speechSpeed
        val result = audioCoordinator.speak(text, languageTag, speed)
        if (result.warningMessage != null) {
            transientState.update { it.copy(voiceWarningBanner = result.warningMessage) }
        }
    }

    fun clearConversationHistory() {
        recentUtterances.clear()
        transientState.update {
            it.copy(
                pendingConfirmation = null,
                lastOpenedAppKey = null
            )
        }
        viewModelScope.launch {
            conversationRepository.clearHistory()
        }
    }

    fun onAppBackgrounded() {
        audioCoordinator.onAppBackgrounded()
    }

    override fun onCleared() {
        super.onCleared()
        audioCoordinator.release()
    }

    private fun formatActionResult(result: ActionExecutionResult): String = when (result) {
        is ActionExecutionResult.Executed -> result.honestStatusMessage
        is ActionExecutionResult.RequiresManualStep -> result.honestStatusMessage
        is ActionExecutionResult.AppNotInstalled -> result.honestStatusMessage
        is ActionExecutionResult.Failed -> result.honestStatusMessage
    }
}

class VaaniXViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val registry = LanguageCapabilityRegistry()
        val speechInput = AndroidSpeechInputEngine(appContext)
        lateinit var coordinator: SpeechAudioCoordinator
        val ttsEngine = AndroidTextToSpeechEngine(appContext) {
            registry.refreshFromDevice(coordinator)
        }
        coordinator = SpeechAudioCoordinator(speechInput, ttsEngine)
        val db = VaaniDatabase.getInstance(appContext)
        val convRepo = RoomConversationRepository(db.conversationDao())
        val prefsStore = DataStorePreferencesRepository(appContext)
        val actionExecutor = AndroidPhoneActionExecutor(appContext)
        val provider = MockMultilingualAssistantProvider()

        return VaaniXViewModel(
            audioCoordinator = coordinator,
            languageRegistry = registry,
            assistantProvider = provider,
            actionExecutor = actionExecutor,
            conversationRepository = convRepo,
            preferencesStore = prefsStore
        ) as T
    }
}

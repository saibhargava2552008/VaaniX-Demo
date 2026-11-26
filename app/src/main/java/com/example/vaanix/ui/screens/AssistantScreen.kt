package com.example.vaanix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalVaaniExtendedColors
import com.example.vaanix.data.ConversationMessageEntity
import com.example.vaanix.language.LocalizedStringsProvider
import com.example.vaanix.speech.SpeechInputState
import com.example.vaanix.ui.PendingActionConfirmation
import com.example.vaanix.ui.VaaniUiState

@Composable
fun AssistantScreen(
    uiState: VaaniUiState,
    onMicClick: () -> Unit,
    onStopAllClick: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onSubmitText: (String) -> Unit,
    onSpeakAgain: (ConversationMessageEntity) -> Unit,
    onConfirmPendingAction: () -> Unit,
    onCancelPendingAction: () -> Unit,
    onToggleTextInput: () -> Unit,
    onDismissWarning: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localized = LocalizedStringsProvider.forLanguage(uiState.selectedLanguage)
    val extendedColors = LocalVaaniExtendedColors.current
    val listState = rememberLazyListState()
    var typedInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Calm live status banner
        VoiceStatusStrip(
            uiState = uiState,
            localizedListening = localized.listeningNow,
            localizedThinking = localized.thinkingNow,
            localizedSpeaking = localized.speakingNow,
            localizedReady = localized.readyHint
        )

        // Microphone permission explanation card (only shown when needed or denied)
        AnimatedVisibility(visible = uiState.showMicPermissionRationale && !uiState.micPermissionGranted) {
            MicPermissionCard(
                message = localized.micPermissionNeeded,
                onAllowClick = onRequestMicPermission
            )
        }

        // Honest voice/capability limitation banner when applicable
        val speechError = uiState.speechInputState as? SpeechInputState.Error
        val bannerMessage = speechError?.userFriendlyMessage ?: uiState.voiceWarningBanner
        AnimatedVisibility(visible = bannerMessage != null) {
            if (bannerMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("voice_notice_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = bannerMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissWarning,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss notice",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Conversation list or warm welcoming onboarding view
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("conversation_list")
        ) {
            if (uiState.messages.isEmpty()) {
                item(key = "welcome_card") {
                    WelcomeHeroCard(
                        greeting = localized.greeting,
                        samplePrompts = localized.samplePrompts,
                        onPromptClick = onSubmitText
                    )
                }
            } else {
                items(uiState.messages, key = { it.id }) { message ->
                    ConversationBubbleCard(
                        message = message,
                        onSpeakAgain = { onSpeakAgain(message) }
                    )
                }
            }
        }

        // Sensitive Action Confirmation Card (WhatsApp message / Phone call)
        AnimatedVisibility(visible = uiState.pendingConfirmation != null) {
            uiState.pendingConfirmation?.let { pending ->
                SensitiveConfirmationCard(
                    pending = pending,
                    onConfirm = onConfirmPendingAction,
                    onCancel = onCancelPendingAction
                )
            }
        }

        // Optional clean Type/Keyboard input bar (for noisy places or unavailable mic/STT)
        AnimatedVisibility(visible = uiState.showTextInputBox) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = typedInput,
                    onValueChange = { typedInput = it },
                    placeholder = {
                        Text(
                            text = "Type your request here...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (typedInput.isNotBlank()) {
                                onSubmitText(typedInput)
                                typedInput = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("assistant_text_input")
                )
                Button(
                    onClick = {
                        if (typedInput.isNotBlank()) {
                            onSubmitText(typedInput)
                            typedInput = ""
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("send_text_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send_message)
                    )
                }
            }
        }

        // Hero Elderly Voice Dock
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, extendedColors.cardBorder),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left control: Type toggle (accessible fallback)
                    OutlinedButton(
                        onClick = onToggleTextInput,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, extendedColors.cardBorder),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_type_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Keyboard,
                            contentDescription = stringResource(R.string.cd_type_toggle),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.showTextInputBox) "Hide" else "Type",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Center HERO Microphone button (88dp touch target + calm listening halo)
                    HeroMicrophoneButton(
                        isListening = uiState.isListening,
                        onClick = onMicClick
                    )

                    // Right control: Clear high-contrast STOP button
                    Button(
                        onClick = onStopAllClick,
                        enabled = uiState.isAudioActive || uiState.isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = extendedColors.stopAccent,
                            contentColor = MaterialTheme.colorScheme.onError,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stop_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = stringResource(R.string.cd_stop_all),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = localized.stopLabel,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState.isListening) localized.listeningNow else localized.tapToSpeak,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (uiState.isListening) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VoiceStatusStrip(
    uiState: VaaniUiState,
    localizedListening: String,
    localizedThinking: String,
    localizedSpeaking: String,
    localizedReady: String
) {
    val extendedColors = LocalVaaniExtendedColors.current
    val listeningPartial = (uiState.speechInputState as? SpeechInputState.Listening)?.partialText.orEmpty()

    val (bgColor, textColor, statusText) = when {
        uiState.isListening -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            if (listeningPartial.isNotBlank()) "\"$listeningPartial\"" else localizedListening
        )
        uiState.isProcessing -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            localizedThinking
        )
        uiState.isSpeaking -> Triple(
            extendedColors.safeContainer,
            extendedColors.onSafeContainer,
            localizedSpeaking
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            localizedReady
        )
    }

    Surface(
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_status_strip")
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun HeroMicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = LocalVaaniExtendedColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "mic_halo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(104.dp)
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(extendedColors.listeningPulse.copy(alpha = 0.24f))
            )
        }

        Surface(
            shape = CircleShape,
            color = if (isListening) extendedColors.stopAccent else MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(84.dp)
                .testTag("hero_mic_button")
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                    contentDescription = if (isListening) {
                        stringResource(R.string.cd_mic_stop)
                    } else {
                        stringResource(R.string.cd_mic_start)
                    },
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun WelcomeHeroCard(
    greeting: String,
    samplePrompts: List<String>,
    onPromptClick: (String) -> Unit
) {
    val extendedColors = LocalVaaniExtendedColors.current
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, extendedColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_vaanix_welcome_1795706654733),
                    contentDescription = stringResource(R.string.cd_welcome_art),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.zero_cost_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = extendedColors.safeAccent
                    )
                }
            }
        }

        Text(
            text = "Tap any example to try VaaniX:",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        samplePrompts.forEachIndexed { index, prompt ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.5.dp, extendedColors.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sample_prompt_$index")
                    .clickable { onPromptClick(prompt) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 15.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationBubbleCard(
    message: ConversationMessageEntity,
    onSpeakAgain: () -> Unit
) {
    val extendedColors = LocalVaaniExtendedColors.current
    val isUser = message.isFromUser

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (isUser) 22.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 22.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            border = BorderStroke(
                width = 1.dp,
                color = if (isUser) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f) else extendedColors.cardBorder
            ),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag(if (isUser) "user_bubble_${message.id}" else "assistant_bubble_${message.id}")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isUser) "You said" else "VaaniX",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                if (!isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (message.isMockResponse) {
                            Text(
                                text = "Offline Guided Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        OutlinedButton(
                            onClick = onSpeakAgain,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("speak_again_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.cd_speak_again),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Read Aloud",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensitiveConfirmationCard(
    pending: PendingActionConfirmation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val extendedColors = LocalVaaniExtendedColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("sensitive_confirmation_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Safety Confirmation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pending.promptMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.5.dp, extendedColors.stopAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = extendedColors.stopAccent),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_action_no")
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm_no),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extendedColors.safeAccent,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("confirm_action_yes")
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm_yes),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MicPermissionCard(
    message: String,
    onAllowClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("mic_permission_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.mic_permission_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$message ${stringResource(R.string.mic_permission_rationale)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAllowClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("allow_mic_button")
            ) {
                Text(
                    text = stringResource(R.string.action_allow_mic),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

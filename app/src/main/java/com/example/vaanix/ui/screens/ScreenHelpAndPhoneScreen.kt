package com.example.vaanix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalVaaniExtendedColors
import com.example.vaanix.language.LocalizedStringsProvider
import com.example.vaanix.phoneactions.PhoneAction
import com.example.vaanix.phoneactions.SettingsTarget
import com.example.vaanix.phoneactions.VolumeDirection
import com.example.vaanix.screenintelligence.ScreenContentAnalyzer
import com.example.vaanix.screenintelligence.ScreenSafetyAlert
import com.example.vaanix.ui.VaaniUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenHelpAndPhoneScreen(
    uiState: VaaniUiState,
    onScreenTextChanged: (String) -> Unit,
    onAnalyzeScreenText: (String?) -> Unit,
    onTriggerQuickAction: (PhoneAction) -> Unit,
    onReadGuideAloud: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localized = LocalizedStringsProvider.forLanguage(uiState.selectedLanguage)
    val extendedColors = LocalVaaniExtendedColors.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Privacy-First Screen & Message Explainer
        item(key = "screen_explainer_section") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, extendedColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PrivacyTip,
                            contentDescription = null,
                            tint = extendedColors.safeAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Explain a Message or Screen",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = localized.screenHelpPrompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Privacy guarantee: VaaniX never silently watches your screen or reads private notifications. It only reads text you paste or share.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extendedColors.safeAccent
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.screenInputText,
                        onValueChange = onScreenTextChanged,
                        placeholder = {
                            Text(
                                text = "Paste an SMS, WhatsApp message, or phone popup here...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("screen_explainer_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScreenContentAnalyzer.sampleMessages.forEachIndexed { idx, (label, sample) ->
                            AssistChip(
                                onClick = {
                                    onScreenTextChanged(sample)
                                    onAnalyzeScreenText(sample)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("sample_screen_chip_$idx")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onAnalyzeScreenText(null) },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("explain_screen_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_explain_text),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Structured Explanation Output
                    uiState.screenExplanation?.let { result ->
                        Spacer(modifier = Modifier.height(14.dp))
                        val isCaution = result.safetyAlert == ScreenSafetyAlert.OTP_OR_SCAM_CAUTION
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCaution) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                extendedColors.safeContainer
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("screen_explanation_result")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCaution) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = "Safety warning",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isCaution) {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        } else {
                                            extendedColors.onSafeContainer
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = result.simpleExplanation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isCaution) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        extendedColors.onSafeContainer
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "What to do: ${result.suggestedNextStep}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCaution) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        extendedColors.onSafeContainer
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Phone Actions (Large Elderly-Friendly Grid)
        item(key = "quick_actions_header") {
            Text(
                text = "Quick Phone Actions",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        item(key = "quick_actions_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        label = "Open WhatsApp",
                        subtitle = "Chat & family",
                        testTag = "quick_action_whatsapp",
                        onClick = {
                            onTriggerQuickAction(
                                PhoneAction.OpenApp(
                                    appKey = "whatsapp",
                                    packageNames = listOf("com.whatsapp", "com.whatsapp.w4b"),
                                    label = "WhatsApp"
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Filled.PlayCircle,
                        label = "YouTube Songs",
                        subtitle = "Devotional music",
                        testTag = "quick_action_youtube",
                        onClick = {
                            onTriggerQuickAction(PhoneAction.SearchYouTube("devotional songs"))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        label = "Volume Up",
                        subtitle = "Make louder",
                        testTag = "quick_action_vol_up",
                        onClick = { onTriggerQuickAction(PhoneAction.AdjustVolume(VolumeDirection.UP)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        label = "Volume Down",
                        subtitle = "Make softer",
                        testTag = "quick_action_vol_down",
                        onClick = { onTriggerQuickAction(PhoneAction.AdjustVolume(VolumeDirection.DOWN)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.Wifi,
                        label = "Wi-Fi Settings",
                        subtitle = "Home internet",
                        testTag = "quick_action_wifi",
                        onClick = { onTriggerQuickAction(PhoneAction.OpenSettings(SettingsTarget.WIFI)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Settings,
                        label = "Phone Settings",
                        subtitle = "All controls",
                        testTag = "quick_action_settings",
                        onClick = { onTriggerQuickAction(PhoneAction.OpenSettings(SettingsTarget.MAIN)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    QuickActionCard(
                        icon = Icons.Filled.Alarm,
                        label = "7:00 AM Alarm",
                        subtitle = "Open Clock app",
                        testTag = "quick_action_alarm",
                        onClick = { onTriggerQuickAction(PhoneAction.SetAlarm(7, 0, "Morning Alarm")) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionCard(
                        icon = Icons.Filled.Call,
                        label = "Phone Dialer",
                        subtitle = "Asks confirmation",
                        testTag = "quick_action_dialer",
                        onClick = { onTriggerQuickAction(PhoneAction.PreparePhoneCall("your contact")) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Step-by-Step Elderly Phone Guides
        item(key = "guides_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Easy Phone Guides",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item(key = "guide_cards") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ElderlyGuideCard(
                    title = "How to send a WhatsApp voice message",
                    steps = "1. Open WhatsApp and tap your family member's name.\n2. Press and hold the green Microphone icon at the bottom right.\n3. Speak clearly, then lift your finger to send.",
                    onReadAloud = onReadGuideAloud
                )
                ElderlyGuideCard(
                    title = "How to stay safe from phone scams & OTP calls",
                    steps = "1. Never share a 4-digit or 6-digit OTP code with anyone on a phone call.\n2. Banks and government offices never ask for your PIN or UPI password.\n3. When unsure, hang up and ask a trusted family member.",
                    onReadAloud = onReadGuideAloud
                )
                ElderlyGuideCard(
                    title = "How to turn on Wi-Fi at home",
                    steps = "1. Tap 'Wi-Fi Settings' above.\n2. Slide the top Wi-Fi switch to ON.\n3. Tap your home Wi-Fi name and wait until it says Connected.",
                    onReadAloud = onReadGuideAloud
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalVaaniExtendedColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.5.dp, extendedColors.cardBorder),
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ElderlyGuideCard(
    title: String,
    steps: String,
    onReadAloud: (String) -> Unit
) {
    val extendedColors = LocalVaaniExtendedColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, extendedColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = steps,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onReadAloud("$title. $steps") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Read guide aloud",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Read Guide Aloud", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

package com.example.vaanix.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalVaaniExtendedColors
import com.example.vaanix.core.VaaniConfig
import com.example.vaanix.language.LanguageCatalog
import com.example.vaanix.language.SupportedLanguage
import com.example.vaanix.speech.SpeechSpeed
import com.example.vaanix.ui.VaaniUiState

@Composable
fun LanguagesAndSettingsScreen(
    uiState: VaaniUiState,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    onRefreshCapabilities: () -> Unit,
    onAutoSpeakChanged: (Boolean) -> Unit,
    onSpeechSpeedSelected: (SpeechSpeed) -> Unit,
    onExtraLargeTextChanged: (Boolean) -> Unit,
    onHighContrastChanged: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalVaaniExtendedColors.current
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Conversation History?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "This removes all saved VaaniX messages from this phone. Your language preference will stay saved.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = extendedColors.stopAccent)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Keep Messages")
                }
            }
        )
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 1. Accessibility & Voice Comfort Section
        item(key = "accessibility_section") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, extendedColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccessibilityNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Voice & Reading Comfort",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    SettingsToggleRow(
                        title = "Speak Replies Automatically",
                        subtitle = "VaaniX reads every reply aloud for you",
                        checked = uiState.preferences.autoSpeakResponses,
                        testTag = "toggle_auto_speak",
                        onCheckedChange = onAutoSpeakChanged
                    )

                    SettingsToggleRow(
                        title = "Extra Large Text",
                        subtitle = "Increases font size across the whole app",
                        checked = uiState.preferences.extraLargeText,
                        testTag = "toggle_extra_large_text",
                        onCheckedChange = onExtraLargeTextChanged
                    )

                    SettingsToggleRow(
                        title = "High Contrast Mode",
                        subtitle = "Stronger text contrast and bold borders",
                        checked = uiState.preferences.highContrastMode,
                        testTag = "toggle_high_contrast",
                        onCheckedChange = onHighContrastChanged
                    )

                    Text(
                        text = "Speaking Speed",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SpeechSpeed.entries.forEach { speed ->
                            val selected = uiState.preferences.speechSpeed == speed
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else extendedColors.cardBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("speed_${speed.name.lowercase()}")
                                    .clickable(role = Role.RadioButton) { onSpeechSpeedSelected(speed) }
                            ) {
                                Text(
                                    text = when (speed) {
                                        SpeechSpeed.SLOW -> "Slower"
                                        SpeechSpeed.NORMAL -> "Normal"
                                        SpeechSpeed.BRISK -> "Faster"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Privacy & Zero-Cost Transparency Card
        item(key = "privacy_section") {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, extendedColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = extendedColors.safeAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Privacy & Assistant Mode",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "• Provider: ${VaaniConfig.llmProvider} (Real paid cloud API calls: ${if (VaaniConfig.allowRealApiCalls) "ON" else "OFF"})\n• Audio privacy: Microphone turns on ONLY when you press Speak.\n• Screen privacy: No silent screen capture or hidden notification reading.\n• Local storage: Language preferences and conversation history stay on this phone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onOpenAccessibilitySettings,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        ) {
                            Text("Android Voice Settings", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            border = BorderStroke(1.5.dp, extendedColors.stopAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = extendedColors.stopAccent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("clear_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear History",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // 3. Pan-India Language Capability Registry
        item(key = "language_registry_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pan-India Languages (${LanguageCatalog.allLanguages.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedButton(
                    onClick = onRefreshCapabilities,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh language check",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        items(LanguageCatalog.allLanguages, key = { it.bcp47Tag }) { lang ->
            val isSelected = lang.bcp47Tag == uiState.selectedLanguage.bcp47Tag
            val capability = uiState.languageCapabilities[lang.bcp47Tag]

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else extendedColors.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_lang_${lang.id}")
                    .clickable(role = Role.RadioButton) { onLanguageSelected(lang) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${lang.nativeName} · ${lang.englishName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Active language",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = capability?.honestSummary ?: "Checking device capability...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            extendedColors.safeAccent
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mixed speech: ${lang.mixedVariantLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isSelected && capability != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Note: ${capability.knownLimitations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

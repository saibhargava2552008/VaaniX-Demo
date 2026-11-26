package com.example.vaanix.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.theme.LocalVaaniExtendedColors
import com.example.ui.theme.VaaniXTheme
import com.example.vaanix.phoneactions.PhoneAction
import com.example.vaanix.phoneactions.SettingsTarget
import com.example.vaanix.ui.components.LanguagePickerSheet
import com.example.vaanix.ui.screens.AssistantScreen
import com.example.vaanix.ui.screens.LanguagesAndSettingsScreen
import com.example.vaanix.ui.screens.ScreenHelpAndPhoneScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaaniXApp(
    viewModel: VaaniXViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.updateMicPermissionStatus(true)
            viewModel.onMicrophoneButtonPressed()
        } else {
            viewModel.onMicPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateMicPermissionStatus(hasMic)
    }

    // Back button returns from secondary tabs to the main Assistant screen cleanly
    BackHandler(enabled = uiState.activeTab != VaaniTab.ASSISTANT) {
        viewModel.selectTab(VaaniTab.ASSISTANT)
    }

    VaaniXTheme(
        highContrast = uiState.preferences.highContrastMode,
        extraLargeText = uiState.preferences.extraLargeText
    ) {
        val extendedColors = LocalVaaniExtendedColors.current

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        actions = {
                            // High-contrast, large-touch Language Selector Pill
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("top_language_pill")
                                    .clickable { viewModel.toggleLanguagePicker(true) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Translate,
                                        contentDescription = stringResource(R.string.cd_language_selector),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.selectedLanguage.nativeName,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.activeTab == VaaniTab.ASSISTANT,
                                onClick = { viewModel.selectTab(VaaniTab.ASSISTANT) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == VaaniTab.ASSISTANT) Icons.Filled.Mic else Icons.Outlined.MicNone,
                                        contentDescription = stringResource(R.string.nav_assistant)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_assistant), style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("nav_tab_assistant")
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == VaaniTab.SCREEN_AND_PHONE,
                                onClick = { viewModel.selectTab(VaaniTab.SCREEN_AND_PHONE) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == VaaniTab.SCREEN_AND_PHONE) Icons.Filled.PhoneAndroid else Icons.Outlined.PhoneAndroid,
                                        contentDescription = stringResource(R.string.nav_screen_help)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_screen_help), style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("nav_tab_screen_phone")
                            )
                            NavigationBarItem(
                                selected = uiState.activeTab == VaaniTab.SETTINGS,
                                onClick = { viewModel.selectTab(VaaniTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == VaaniTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.nav_settings)
                                    )
                                },
                                label = { Text("Languages", style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("nav_tab_settings")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            NavigationRailItem(
                                selected = uiState.activeTab == VaaniTab.ASSISTANT,
                                onClick = { viewModel.selectTab(VaaniTab.ASSISTANT) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == VaaniTab.ASSISTANT) Icons.Filled.Mic else Icons.Outlined.MicNone,
                                        contentDescription = stringResource(R.string.nav_assistant)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_assistant)) }
                            )
                            NavigationRailItem(
                                selected = uiState.activeTab == VaaniTab.SCREEN_AND_PHONE,
                                onClick = { viewModel.selectTab(VaaniTab.SCREEN_AND_PHONE) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == VaaniTab.SCREEN_AND_PHONE) Icons.Filled.PhoneAndroid else Icons.Outlined.PhoneAndroid,
                                        contentDescription = stringResource(R.string.nav_screen_help)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_screen_help)) }
                            )
                            NavigationRailItem(
                                selected = uiState.activeTab == VaaniTab.SETTINGS,
                                onClick = { viewModel.selectTab(VaaniTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.activeTab == VaaniTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = stringResource(R.string.nav_settings)
                                    )
                                },
                                label = { Text("Languages") }
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val contentModifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)

                        when (uiState.activeTab) {
                            VaaniTab.ASSISTANT -> AssistantScreen(
                                uiState = uiState,
                                onMicClick = {
                                    if (uiState.micPermissionGranted) {
                                        viewModel.onMicrophoneButtonPressed()
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                onStopAllClick = viewModel::onStopAllPressed,
                                onRequestMicPermission = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                onSubmitText = viewModel::submitUserUtterance,
                                onSpeakAgain = { msg -> viewModel.speakMessage(msg.text, msg.languageTag) },
                                onConfirmPendingAction = viewModel::confirmPendingAction,
                                onCancelPendingAction = viewModel::cancelPendingAction,
                                onToggleTextInput = viewModel::toggleTextInputBox,
                                onDismissWarning = {
                                    viewModel.dismissVoiceWarning()
                                },
                                modifier = contentModifier
                            )

                            VaaniTab.SCREEN_AND_PHONE -> ScreenHelpAndPhoneScreen(
                                uiState = uiState,
                                onScreenTextChanged = viewModel::updateScreenInputText,
                                onAnalyzeScreenText = viewModel::analyzeScreenText,
                                onTriggerQuickAction = viewModel::triggerQuickPhoneAction,
                                onReadGuideAloud = { text -> viewModel.speakMessage(text) },
                                modifier = contentModifier
                            )

                            VaaniTab.SETTINGS -> LanguagesAndSettingsScreen(
                                uiState = uiState,
                                onLanguageSelected = viewModel::onLanguageSelected,
                                onRefreshCapabilities = viewModel::refreshLanguageCapabilities,
                                onAutoSpeakChanged = viewModel::setAutoSpeak,
                                onSpeechSpeedSelected = viewModel::setSpeechSpeed,
                                onExtraLargeTextChanged = viewModel::setExtraLargeText,
                                onHighContrastChanged = viewModel::setHighContrastMode,
                                onClearHistory = viewModel::clearConversationHistory,
                                onOpenAccessibilitySettings = {
                                    viewModel.triggerQuickPhoneAction(
                                        PhoneAction.OpenSettings(SettingsTarget.ACCESSIBILITY)
                                    )
                                },
                                modifier = contentModifier
                            )
                        }
                    }
                }

                if (uiState.showLanguagePickerSheet) {
                    LanguagePickerSheet(
                        selectedLanguage = uiState.selectedLanguage,
                        capabilities = uiState.languageCapabilities,
                        onLanguageSelected = viewModel::onLanguageSelected,
                        onDismiss = { viewModel.toggleLanguagePicker(false) }
                    )
                }
            }
        }
    }
}

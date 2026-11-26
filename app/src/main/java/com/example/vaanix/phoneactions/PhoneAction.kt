package com.example.vaanix.phoneactions

enum class ActionSafetyLevel {
    SAFE,
    SENSITIVE
}

enum class SettingsTarget(val displayName: String) {
    MAIN("Phone Settings"),
    WIFI("Wi-Fi Settings"),
    SOUND("Sound & Volume Settings"),
    DISPLAY("Screen & Text Size Settings"),
    ACCESSIBILITY("Accessibility Settings")
}

enum class VolumeDirection {
    UP,
    DOWN
}

enum class PhoneGuideTopic(val title: String) {
    WHATSAPP_BASICS("How to use WhatsApp"),
    YOUTUBE_BASICS("How to watch videos on YouTube"),
    WIFI_AND_DATA("How to turn on Wi-Fi"),
    PHONE_CALLS("How to make a phone call safely"),
    GENERAL_PHONE("Basic smartphone guide")
}

/**
 * Strongly-typed smartphone actions that VaaniX can perform via supported Android APIs.
 * Sensitive actions (calls, messages) always set requiresConfirmation = true.
 */
sealed class PhoneAction {
    abstract val label: String
    abstract val safetyLevel: ActionSafetyLevel
    val requiresConfirmation: Boolean
        get() = safetyLevel == ActionSafetyLevel.SENSITIVE

    data class OpenApp(
        val appKey: String,
        val packageNames: List<String>,
        override val label: String
    ) : PhoneAction() {
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SAFE
    }

    data class SearchYouTube(
        val query: String
    ) : PhoneAction() {
        override val label: String = "Search YouTube for \"$query\""
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SAFE
    }

    data class AdjustVolume(
        val direction: VolumeDirection
    ) : PhoneAction() {
        override val label: String = if (direction == VolumeDirection.UP) "Increase Volume" else "Decrease Volume"
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SAFE
    }

    data class OpenSettings(
        val target: SettingsTarget
    ) : PhoneAction() {
        override val label: String = "Open ${target.displayName}"
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SAFE
    }

    data class PrepareWhatsAppMessage(
        val recipientHint: String,
        val draftText: String
    ) : PhoneAction() {
        override val label: String = "Prepare WhatsApp message for $recipientHint"
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SENSITIVE
    }

    data class PreparePhoneCall(
        val targetNameOrNumber: String
    ) : PhoneAction() {
        override val label: String = "Open Phone Dialer for $targetNameOrNumber"
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SENSITIVE
    }

    data class SetAlarm(
        val hour24: Int,
        val minute: Int,
        val alarmLabel: String
    ) : PhoneAction() {
        override val label: String = "Set alarm for %02d:%02d".format(hour24, minute)
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SAFE
    }

    data class ShowStepByStepGuide(
        val topic: PhoneGuideTopic
    ) : PhoneAction() {
        override val label: String = topic.title
        override val safetyLevel: ActionSafetyLevel = ActionSafetyLevel.SAFE
    }
}

sealed class ActionExecutionResult {
    data class Executed(val honestStatusMessage: String) : ActionExecutionResult()
    data class RequiresManualStep(val honestStatusMessage: String) : ActionExecutionResult()
    data class AppNotInstalled(val appName: String, val honestStatusMessage: String) : ActionExecutionResult()
    data class Failed(val honestStatusMessage: String) : ActionExecutionResult()
}

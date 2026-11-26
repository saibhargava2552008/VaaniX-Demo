package com.example.vaanix.phoneactions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import java.net.URLEncoder
import kotlin.math.roundToInt

interface PhoneActionExecutor {
    fun execute(action: PhoneAction): ActionExecutionResult
}

/**
 * Executes real Android intents and system APIs safely.
 * Never claims an action succeeded if Android could not launch the target activity.
 * Sensitive actions open the native compose/dialer screen so the user retains final control.
 */
class AndroidPhoneActionExecutor(
    private val context: Context
) : PhoneActionExecutor {

    override fun execute(action: PhoneAction): ActionExecutionResult {
        return when (action) {
            is PhoneAction.OpenApp -> launchInstalledApp(action)
            is PhoneAction.SearchYouTube -> launchYouTubeSearch(action.query)
            is PhoneAction.AdjustVolume -> adjustMediaVolume(action.direction)
            is PhoneAction.OpenSettings -> launchSettings(action.target)
            is PhoneAction.PrepareWhatsAppMessage -> launchWhatsAppDraft(action)
            is PhoneAction.PreparePhoneCall -> launchDialer(action.targetNameOrNumber)
            is PhoneAction.SetAlarm -> launchAlarmIntent(action)
            is PhoneAction.ShowStepByStepGuide -> ActionExecutionResult.Executed(
                "Here is your simple guide for ${action.topic.title}."
            )
        }
    }

    private fun launchInstalledApp(action: PhoneAction.OpenApp): ActionExecutionResult {
        val pm = context.packageManager
        for (pkg in action.packageNames) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return startIntentSafely(
                    intent = launchIntent,
                    successMessage = "Opened ${action.label} on your phone.",
                    failureMessage = "Android could not open ${action.label} right now."
                )
            }
        }
        return ActionExecutionResult.AppNotInstalled(
            appName = action.label,
            honestStatusMessage = "${action.label} does not appear to be installed on this phone. Please install it from the Play Store or check if it is disabled."
        )
    }

    private fun launchYouTubeSearch(query: String): ActionExecutionResult {
        val encoded = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Throwable) {
            query.replace(" ", "+")
        }
        val uri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startIntentSafely(
            intent = intent,
            successMessage = "Opened YouTube search for \"$query\". Tap any video to play it.",
            failureMessage = "Could not open YouTube or a web browser on this phone."
        )
    }

    private fun adjustMediaVolume(direction: VolumeDirection): ActionExecutionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ActionExecutionResult.Failed("Could not access the phone's volume control.")

            val adjustFlag = if (direction == VolumeDirection.UP) {
                AudioManager.ADJUST_RAISE
            } else {
                AudioManager.ADJUST_LOWER
            }
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                adjustFlag,
                AudioManager.FLAG_SHOW_UI
            )
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val percent = ((current.toFloat() / max.toFloat()) * 100f).roundToInt()
            val verb = if (direction == VolumeDirection.UP) "Increased" else "Decreased"
            ActionExecutionResult.Executed("$verb media volume. Current volume is around $percent%.")
        } catch (_: Throwable) {
            ActionExecutionResult.Failed("Could not change volume right now. Please use the side buttons on your phone.")
        }
    }

    private fun launchSettings(target: SettingsTarget): ActionExecutionResult {
        val actionStr = when (target) {
            SettingsTarget.MAIN -> Settings.ACTION_SETTINGS
            SettingsTarget.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsTarget.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SettingsTarget.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsTarget.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
        }
        val intent = Intent(actionStr).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return startIntentSafely(
            intent = intent,
            successMessage = "Opened ${target.displayName}. Use the Back button whenever you want to return to VaaniX.",
            failureMessage = "Could not open ${target.displayName} automatically on this phone."
        )
    }

    private fun launchWhatsAppDraft(action: PhoneAction.PrepareWhatsAppMessage): ActionExecutionResult {
        val pm = context.packageManager
        val whatsAppPkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull {
            pm.getLaunchIntentForPackage(it) != null
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, action.draftText)
            if (whatsAppPkg != null) {
                setPackage(whatsAppPkg)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(shareIntent)
            if (whatsAppPkg != null) {
                ActionExecutionResult.RequiresManualStep(
                    "Opened WhatsApp with your draft message. Please select \"${action.recipientHint}\" and tap Send to confirm."
                )
            } else {
                ActionExecutionResult.RequiresManualStep(
                    "WhatsApp is not installed on this phone, so I opened the share menu with your message draft. Choose your messaging app to send."
                )
            }
        } catch (_: ActivityNotFoundException) {
            ActionExecutionResult.AppNotInstalled(
                appName = "WhatsApp",
                honestStatusMessage = "WhatsApp is not installed on this phone. Please install WhatsApp first, or copy your draft message."
            )
        } catch (_: Throwable) {
            ActionExecutionResult.Failed("Could not open WhatsApp compose screen right now.")
        }
    }

    private fun launchDialer(targetNameOrNumber: String): ActionExecutionResult {
        val digitsOnly = targetNameOrNumber.filter { it.isDigit() || it == '+' }
        val dialUri = if (digitsOnly.length >= 3) {
            Uri.parse("tel:$digitsOnly")
        } else {
            Uri.parse("tel:")
        }
        // Uses ACTION_DIAL (zero dangerous CALL_PHONE permission; user presses Call safely)
        val intent = Intent(Intent.ACTION_DIAL, dialUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val extraHint = if (digitsOnly.length >= 3) {
            "Opened the Phone Dialer with $digitsOnly. Press the green Call button when you are ready."
        } else {
            "Opened the Phone Dialer. Please choose \"$targetNameOrNumber\" from your Contacts and press Call."
        }
        return startIntentSafely(
            intent = intent,
            successMessage = extraHint,
            failureMessage = "Could not open the Phone Dialer on this device."
        )
    }

    private fun launchAlarmIntent(action: PhoneAction.SetAlarm): ActionExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, action.hour24)
            putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, action.alarmLabel)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startIntentSafely(
            intent = intent,
            successMessage = "Opened your Clock app to confirm the %02d:%02d alarm.".format(action.hour24, action.minute),
            failureMessage = "No compatible Clock app responded to set the alarm automatically. Please open your Clock app directly."
        )
    }

    private fun startIntentSafely(
        intent: Intent,
        successMessage: String,
        failureMessage: String
    ): ActionExecutionResult {
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionExecutionResult.Executed(successMessage)
            } else {
                ActionExecutionResult.Failed(failureMessage)
            }
        } catch (_: Throwable) {
            ActionExecutionResult.Failed(failureMessage)
        }
    }
}

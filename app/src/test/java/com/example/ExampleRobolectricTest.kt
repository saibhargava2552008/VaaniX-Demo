package com.example

import android.content.Context
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import com.example.vaanix.phoneactions.ActionExecutionResult
import com.example.vaanix.phoneactions.AndroidPhoneActionExecutor
import com.example.vaanix.phoneactions.PhoneAction
import com.example.vaanix.speech.AndroidSpeechInputEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `app name resource matches VaaniX`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VaaniX", appName)
    }

    @Test
    fun `phone action executor honestly reports when app is not installed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val executor = AndroidPhoneActionExecutor(context)
        val result = executor.execute(
            PhoneAction.OpenApp(
                appKey = "whatsapp",
                packageNames = listOf("com.whatsapp.nonexistent.test"),
                label = "WhatsApp"
            )
        )
        assertTrue(result is ActionExecutionResult.AppNotInstalled)
        val msg = (result as ActionExecutionResult.AppNotInstalled).honestStatusMessage
        assertTrue(msg.contains("not appear to be installed", ignoreCase = true))
    }

    @Test
    fun `speech error codes map to elderly friendly plain language`() {
        val permMsg = AndroidSpeechInputEngine.mapErrorToFriendlyMessage(
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
        )
        val noMatchMsg = AndroidSpeechInputEngine.mapErrorToFriendlyMessage(
            SpeechRecognizer.ERROR_NO_MATCH
        )
        val networkMsg = AndroidSpeechInputEngine.mapErrorToFriendlyMessage(
            SpeechRecognizer.ERROR_NETWORK
        )
        assertTrue(permMsg.contains("microphone permission", ignoreCase = true))
        assertTrue(noMatchMsg.contains("try again", ignoreCase = true))
        assertTrue(networkMsg.contains("internet", ignoreCase = true))
    }
}

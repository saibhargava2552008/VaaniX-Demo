package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.vaanix.ui.VaaniXApp
import com.example.vaanix.ui.VaaniXViewModel
import com.example.vaanix.ui.VaaniXViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: VaaniXViewModel by viewModels {
        VaaniXViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShareIntent(intent)
        setContent {
            VaaniXApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        // Privacy & lifecycle rule: immediately cancel microphone & stop TTS when backgrounded
        viewModel.onAppBackgrounded()
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleIncomingSharedText(sharedText)
            }
        }
    }
}

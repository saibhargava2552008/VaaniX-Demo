package com.example.vaanix.core

import com.example.BuildConfig

/**
 * Central configuration enforcing VaaniX's zero-cost development policy
 * and privacy-first architecture boundaries.
 */
object VaaniConfig {
    const val APP_NAME: String = "VaaniX"
    const val TAGLINE: String = "Your Language. Your Voice. Your Access."

    // Preserved zero-cost settings (no paid cloud APIs without explicit user approval)
    val llmProvider: String = BuildConfig.LLM_PROVIDER
    val allowRealApiCalls: Boolean = BuildConfig.ALLOW_REAL_API_CALLS

    val isMockProviderActive: Boolean
        get() = llmProvider.equals("mock", ignoreCase = true) || !allowRealApiCalls

    const val MAX_CONVERSATION_CONTEXT_TURNS = 8
    const val DEFAULT_LANGUAGE_TAG = "en-IN"
}

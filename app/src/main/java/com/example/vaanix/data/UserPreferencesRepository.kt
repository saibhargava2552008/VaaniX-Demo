package com.example.vaanix.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.vaanix.core.VaaniConfig
import com.example.vaanix.speech.SpeechSpeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private val Context.vaaniDataStore: DataStore<Preferences> by preferencesDataStore(name = "vaanix_preferences")

data class UserPreferences(
    val selectedLanguageTag: String = VaaniConfig.DEFAULT_LANGUAGE_TAG,
    val autoSpeakResponses: Boolean = true,
    val speechSpeed: SpeechSpeed = SpeechSpeed.SLOW,
    val extraLargeText: Boolean = false,
    val highContrastMode: Boolean = false
)

interface PreferencesStore {
    val preferences: Flow<UserPreferences>
    suspend fun setLanguageTag(tag: String)
    suspend fun setAutoSpeak(enabled: Boolean)
    suspend fun setSpeechSpeed(speed: SpeechSpeed)
    suspend fun setExtraLargeText(enabled: Boolean)
    suspend fun setHighContrastMode(enabled: Boolean)
}

class DataStorePreferencesRepository(
    private val context: Context
) : PreferencesStore {

    private object Keys {
        val LANGUAGE_TAG = stringPreferencesKey("selected_language_tag")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_responses")
        val SPEECH_SPEED = stringPreferencesKey("speech_speed")
        val EXTRA_LARGE_TEXT = booleanPreferencesKey("extra_large_text")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast_mode")
    }

    override val preferences: Flow<UserPreferences> = context.vaaniDataStore.data.map { prefs ->
        val speedName = prefs[Keys.SPEECH_SPEED] ?: SpeechSpeed.SLOW.name
        val parsedSpeed = SpeechSpeed.entries.firstOrNull { it.name == speedName } ?: SpeechSpeed.SLOW
        UserPreferences(
            selectedLanguageTag = prefs[Keys.LANGUAGE_TAG] ?: VaaniConfig.DEFAULT_LANGUAGE_TAG,
            autoSpeakResponses = prefs[Keys.AUTO_SPEAK] ?: true,
            speechSpeed = parsedSpeed,
            extraLargeText = prefs[Keys.EXTRA_LARGE_TEXT] ?: false,
            highContrastMode = prefs[Keys.HIGH_CONTRAST] ?: false
        )
    }

    override suspend fun setLanguageTag(tag: String) {
        context.vaaniDataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }

    override suspend fun setAutoSpeak(enabled: Boolean) {
        context.vaaniDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    override suspend fun setSpeechSpeed(speed: SpeechSpeed) {
        context.vaaniDataStore.edit { it[Keys.SPEECH_SPEED] = speed.name }
    }

    override suspend fun setExtraLargeText(enabled: Boolean) {
        context.vaaniDataStore.edit { it[Keys.EXTRA_LARGE_TEXT] = enabled }
    }

    override suspend fun setHighContrastMode(enabled: Boolean) {
        context.vaaniDataStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }
}

class InMemoryPreferencesStore(
    initial: UserPreferences = UserPreferences()
) : PreferencesStore {
    private val _prefs = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setLanguageTag(tag: String) {
        _prefs.update { it.copy(selectedLanguageTag = tag) }
    }

    override suspend fun setAutoSpeak(enabled: Boolean) {
        _prefs.update { it.copy(autoSpeakResponses = enabled) }
    }

    override suspend fun setSpeechSpeed(speed: SpeechSpeed) {
        _prefs.update { it.copy(speechSpeed = speed) }
    }

    override suspend fun setExtraLargeText(enabled: Boolean) {
        _prefs.update { it.copy(extraLargeText = enabled) }
    }

    override suspend fun setHighContrastMode(enabled: Boolean) {
        _prefs.update { it.copy(highContrastMode = enabled) }
    }
}

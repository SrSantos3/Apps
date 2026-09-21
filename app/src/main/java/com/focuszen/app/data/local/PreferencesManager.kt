package com.focuszen.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "focus_zen_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_STRICT_MODE = booleanPreferencesKey("strict_mode_enabled")
        val KEY_DEFAULT_FRICTION_SECONDS = intPreferencesKey("default_friction_seconds")
        val KEY_EXTENSION_DELAY_SECONDS = intPreferencesKey("extension_delay_seconds")
        val KEY_VIBRATION_GUIDE = booleanPreferencesKey("vibration_guide")
    }

    val isStrictModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STRICT_MODE] ?: true
    }

    val defaultFrictionSeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_FRICTION_SECONDS] ?: 10
    }

    val extensionDelaySeconds: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_EXTENSION_DELAY_SECONDS] ?: 60
    }

    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_GUIDE] ?: true
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_STRICT_MODE] = enabled }
    }

    suspend fun setDefaultFrictionSeconds(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_DEFAULT_FRICTION_SECONDS] = seconds }
    }

    suspend fun setExtensionDelaySeconds(seconds: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_EXTENSION_DELAY_SECONDS] = seconds }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_VIBRATION_GUIDE] = enabled }
    }
}

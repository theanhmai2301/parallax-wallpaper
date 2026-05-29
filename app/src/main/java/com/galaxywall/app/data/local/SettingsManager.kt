package com.galaxywall.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val darkMode: Boolean = true,
    val dynamicColor: Boolean = false,
    val parallaxEnabled: Boolean = true,
    val sensitivity: Float = 0.5f
)

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PARALLAX_ENABLED = booleanPreferencesKey("parallax_enabled")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            parallaxEnabled = prefs[Keys.PARALLAX_ENABLED] ?: true,
            sensitivity = prefs[Keys.SENSITIVITY] ?: 0.5f
        )
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setParallaxEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PARALLAX_ENABLED] = enabled }
    }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = value.coerceIn(0f, 1f) }
    }
}

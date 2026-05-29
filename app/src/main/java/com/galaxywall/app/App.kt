package com.galaxywall.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.galaxywall.app.data.local.SettingsManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        applyStoredTheme()
        applyDynamicColor()
    }

    private fun applyStoredTheme() {
        val darkMode = runBlocking { settingsManager.settings.first().darkMode }
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** Re-evaluated whenever an activity is (re)created, so toggling + recreate() takes effect. */
    private fun applyDynamicColor() {
        val options = DynamicColorsOptions.Builder()
            .setPrecondition { _, _ ->
                runBlocking { settingsManager.settings.first().dynamicColor }
            }
            .build()
        DynamicColors.applyToActivitiesIfAvailable(this, options)
    }
}

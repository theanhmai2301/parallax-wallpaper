package com.galaxywall.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.data.local.AppSettings
import com.galaxywall.app.data.local.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val state: StateFlow<AppSettings> =
        settingsManager.settings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setDarkMode(enabled) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsManager.setDynamicColor(enabled) }
    fun setParallax(enabled: Boolean) = viewModelScope.launch { settingsManager.setParallaxEnabled(enabled) }
    fun setSensitivity(value: Float) = viewModelScope.launch { settingsManager.setSensitivity(value) }
}

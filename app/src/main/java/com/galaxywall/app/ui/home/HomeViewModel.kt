package com.galaxywall.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import com.galaxywall.app.data.repository.WallpaperRepository
import com.galaxywall.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WallpaperRepository
) : ViewModel() {

    enum class Mode { HOME, CATEGORY }

    /** Theme chips shown only in Category mode (no "All"). */
    val themes: List<WallpaperCategory> = listOf(
        WallpaperCategory.SPORT,
        WallpaperCategory.GALAXY,
        WallpaperCategory.ANIME,
        WallpaperCategory.CAR,
        WallpaperCategory.SILLYSMILE
    )

    private val _mode = MutableStateFlow(Mode.HOME)
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    private val _theme = MutableStateFlow(themes.first())
    val theme: StateFlow<WallpaperCategory> = _theme.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<List<Wallpaper>>> =
        combine(_mode, _theme, refreshTrigger) { mode, theme, _ -> mode to theme }
            .flatMapLatest { (mode, theme) ->
                // HOME = a curated random mix (a few per category); CATEGORY = one theme.
                val source = if (mode == Mode.HOME) {
                    repository.observeHomeFeed()
                } else {
                    repository.observeWallpapers(null, theme, "")
                }
                source.map<List<Wallpaper>, UiState<List<Wallpaper>>> { list ->
                    if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error(it.message ?: "Something went wrong")) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun setMode(mode: Mode) {
        _mode.value = mode
    }

    fun setTheme(theme: WallpaperCategory) {
        _theme.value = theme
    }

    fun refresh() {
        repository.invalidate()
        refreshTrigger.value++
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            repository.setFavorite(wallpaper.id, !wallpaper.isFavorite)
        }
    }
}

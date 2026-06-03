package com.galaxywall.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import com.galaxywall.app.data.repository.WallpaperRepository
import com.galaxywall.app.util.NetworkMonitor
import com.galaxywall.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val networkMonitor: NetworkMonitor
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

    /** How many Home-feed items are currently revealed (grows as the user scrolls to the bottom). */
    private val _homeLimit = MutableStateFlow(HOME_PAGE_SIZE)

    /** True while the next page is being revealed, so the UI can show a bottom loading spinner. */
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    /** Size of the full Home feed, captured on each emission so [loadMoreHome] knows when to stop. */
    @Volatile
    private var fullHomeCount = 0

    val uiState: StateFlow<UiState<List<Wallpaper>>> =
        combine(_mode, _theme, refreshTrigger) { mode, theme, _ -> mode to theme }
            .flatMapLatest { (mode, theme) ->
                // HOME = a curated mix revealed page by page (infinite scroll); CATEGORY = one theme.
                val source = if (mode == Mode.HOME) {
                    combine(repository.observeHomeFeed(), _homeLimit) { full, limit ->
                        fullHomeCount = full.size
                        full.take(limit)
                    }
                } else {
                    repository.observeWallpapers(null, theme, "")
                }
                source.map<List<Wallpaper>, UiState<List<Wallpaper>>> { list ->
                    when {
                        list.isNotEmpty() -> UiState.Success(list)
                        // No data AND offline -> tell the user it's a connection problem.
                        !networkMonitor.isOnline() -> UiState.NoNetwork
                        else -> UiState.Empty
                    }
                }
                    .onStart { emit(UiState.Loading) }
                    .catch {
                        emit(
                            if (!networkMonitor.isOnline()) UiState.NoNetwork
                            else UiState.Error(it.message ?: "Something went wrong")
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        // When connectivity comes back while we're showing the "no internet" screen, reload itself.
        networkMonitor.observe()
            .onEach { online -> if (online && uiState.value is UiState.NoNetwork) refresh() }
            .launchIn(viewModelScope)
    }

    /** Reveals the next page of Home items when the user scrolls to the bottom. No-op when there's
     *  nothing more to show or a page is already loading. */
    fun loadMoreHome() {
        if (_mode.value != Mode.HOME || _loadingMore.value) return
        // fullHomeCount in 1..limit means everything is already revealed.
        if (fullHomeCount in 1.._homeLimit.value) return
        viewModelScope.launch {
            _loadingMore.value = true
            // Brief pause so the spinner is visible (the catalog is cached, so reveal is instant).
            delay(450)
            _homeLimit.value = _homeLimit.value + HOME_PAGE_SIZE
            _loadingMore.value = false
        }
    }

    fun setMode(mode: Mode) {
        // Restart the Home feed from the first page each time Home is (re)entered.
        if (mode == Mode.HOME) _homeLimit.value = HOME_PAGE_SIZE
        _mode.value = mode
    }

    fun setTheme(theme: WallpaperCategory) {
        _theme.value = theme
    }

    fun refresh() {
        _homeLimit.value = HOME_PAGE_SIZE
        _loadingMore.value = false
        repository.invalidate()
        refreshTrigger.value++
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            repository.setFavorite(wallpaper.id, !wallpaper.isFavorite)
        }
    }

    private companion object {
        const val HOME_PAGE_SIZE = 12
    }
}

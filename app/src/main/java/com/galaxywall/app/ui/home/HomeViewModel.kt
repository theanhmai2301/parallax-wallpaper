package com.galaxywall.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.repository.WallpaperRepository
import com.galaxywall.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: WallpaperRepository
) : ViewModel() {

    /** Top filter bar: by content type (parallax / photo / video) rather than theme. */
    enum class ContentFilter(val label: String, val type: ContentType?) {
        ALL("All", null),
        PARALLAX("Parallax", ContentType.PARALLAX),
        IMAGE("Photo", ContentType.IMAGE),
        VIDEO("Video", ContentType.VIDEO)
    }

    val filters: List<ContentFilter> = ContentFilter.entries

    private val _filter = MutableStateFlow(ContentFilter.ALL)
    val filter: StateFlow<ContentFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<UiState<List<Wallpaper>>> =
        combine(_filter, _query.debounce(220), refreshTrigger) { f, q, _ -> f to q }
            .flatMapLatest { (f, q) ->
                repository.observeWallpapers(f.type, q)
                    .map<List<Wallpaper>, UiState<List<Wallpaper>>> { list ->
                        if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                    }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error(it.message ?: "Something went wrong")) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun setFilter(filter: ContentFilter) {
        _filter.value = filter
    }

    fun setQuery(query: String) {
        _query.value = query
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

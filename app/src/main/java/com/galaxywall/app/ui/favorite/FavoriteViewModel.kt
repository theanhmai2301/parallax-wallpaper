package com.galaxywall.app.ui.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.repository.WallpaperRepository
import com.galaxywall.app.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repository: WallpaperRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<List<Wallpaper>>> =
        repository.observeFavorites()
            .map<List<Wallpaper>, UiState<List<Wallpaper>>> { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            repository.setFavorite(wallpaper.id, !wallpaper.isFavorite)
        }
    }
}

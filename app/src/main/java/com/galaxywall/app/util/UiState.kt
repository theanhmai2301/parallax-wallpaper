package com.galaxywall.app.util

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    /** No data could be loaded because the device is offline. */
    data object NoNetwork : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}

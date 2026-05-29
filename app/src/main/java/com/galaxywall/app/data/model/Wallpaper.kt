package com.galaxywall.app.data.model

/**
 * A parallax wallpaper. [layerUris] is ordered bottom (background) -> top (foreground); each entry
 * is a fully-qualified remote image URL ("https://...") that Coil can load. [thumbUri] is the grid
 * preview.
 */
data class Wallpaper(
    val id: String,
    val title: String,
    val category: WallpaperCategory,
    val thumbUri: String,
    val layerUris: List<String>,
    val isFavorite: Boolean = false
) {
    val backUri: String? get() = layerUris.firstOrNull()
    val frontUri: String? get() = layerUris.getOrNull(1)
    val resolutionLabel: String get() = "4K"
}

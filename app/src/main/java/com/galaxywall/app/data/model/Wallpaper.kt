package com.galaxywall.app.data.model

/**
 * A parallax wallpaper. [layerUris] is ordered bottom (background) -> top (foreground); each entry
 * is a fully-qualified URI that Coil can load — either a bundled asset
 * ("file:///android_asset/...") or a remote image ("https://..."). [thumbUri] is the grid preview.
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

    companion object {
        const val ASSET_PREFIX = "file:///android_asset/"
    }
}

fun String.asAssetUri(): String =
    if (startsWith("http") || startsWith("file://")) this else Wallpaper.ASSET_PREFIX + this

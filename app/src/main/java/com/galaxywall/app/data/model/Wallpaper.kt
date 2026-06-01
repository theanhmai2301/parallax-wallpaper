package com.galaxywall.app.data.model

/** What kind of wallpaper an item is and how it gets applied. */
enum class ContentType { PARALLAX, IMAGE, VIDEO }

/**
 * A wallpaper item from the API. [type] decides how it is applied:
 *  - PARALLAX: built from [layerUris] (bottom -> top) via the builder flow.
 *  - IMAGE: a static image at [sourceUrl], applied with WallpaperManager.
 *  - VIDEO: an MP4 at [sourceUrl], applied as a looping live wallpaper.
 * [thumbUri] is always the grid preview.
 */
data class Wallpaper(
    val id: String,
    val title: String,
    val category: WallpaperCategory,
    val thumbUri: String,
    val layerUris: List<String> = emptyList(),
    val type: ContentType = ContentType.PARALLAX,
    val sourceUrl: String? = null,
    val isFavorite: Boolean = false
) {
    val backUri: String? get() = layerUris.firstOrNull()
    val frontUri: String? get() = layerUris.getOrNull(1)
    val resolutionLabel: String get() = "4K"
}

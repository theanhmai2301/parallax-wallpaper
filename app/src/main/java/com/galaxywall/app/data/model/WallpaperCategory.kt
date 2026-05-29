package com.galaxywall.app.data.model

enum class WallpaperCategory(val key: String, val label: String) {
    ALL("all", "All"),
    SPORT("sport", "Sport"),
    GALAXY("galaxy", "Galaxy"),
    ANIME("anime", "Anime");

    companion object {
        fun fromKey(key: String): WallpaperCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ALL
    }
}

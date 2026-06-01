package com.galaxywall.app.data.model

enum class WallpaperCategory(val key: String, val label: String) {
    ALL("all", "All"),
    CAR("car", "Car"),
    SILLYSMILE("sillysmile", "Smile"),
    SPORT("sport", "Sport"),
    FISH("fish", "Fish"),
    TRENDING("trending_funky", "Trending"),
    ANIME("anime", "Anime"),
    GALAXY("galaxy", "Galaxy");

    companion object {
        fun fromKey(key: String): WallpaperCategory =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: ALL
    }
}

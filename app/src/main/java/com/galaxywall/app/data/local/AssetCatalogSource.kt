package com.galaxywall.app.data.local

import android.content.Context
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import com.galaxywall.app.data.model.asAssetUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Reads and parses the bundled catalog.json describing every wallpaper and its layers. */
@Singleton
class AssetCatalogSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var cache: List<Wallpaper>? = null

    suspend fun loadAll(): List<Wallpaper> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            cache ?: parse().also { cache = it }
        }
    }

    private fun parse(): List<Wallpaper> {
        val json = context.assets.open(CATALOG_FILE).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val array = root.getJSONArray("wallpapers")
        val result = ArrayList<Wallpaper>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Wallpaper(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    category = WallpaperCategory.fromKey(obj.getString("category")),
                    thumbUri = obj.getString("thumb").asAssetUri(),
                    // bottom (background, *1.png) -> top (subject, *2.png)
                    layerUris = listOf(
                        obj.getString("back").asAssetUri(),
                        obj.getString("front").asAssetUri()
                    )
                )
            )
        }
        return result
    }

    companion object {
        private const val CATALOG_FILE = "catalog.json"
    }
}

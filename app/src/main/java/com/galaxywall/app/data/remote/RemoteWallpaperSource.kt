package com.galaxywall.app.data.remote

import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches parallax items from the DataWallpaper API and groups their layers into [Wallpaper]s.
 *
 * Files follow `<Category><GroupId><LayerId>.{png|jpg}` (e.g. Anime53.jpg). Items sharing a group
 * key (Anime5) are merged: layer 1 = background (bottom), layer 2 = subject (top), layer 3 = the
 * pre-composed JPG used as the grid thumbnail.
 */
@Singleton
class RemoteWallpaperSource @Inject constructor(
    private val api: WallpaperApi
) {
    suspend fun loadParallax(): List<Wallpaper> =
        groupItems(api.listParallax(limit = 200).data)

    /** Static images from /api/images — extra source images for the layer picker library. */
    suspend fun loadImages(): List<String> =
        api.listImages(limit = 200).data.mapNotNull { it.url }

    private fun groupItems(items: List<RemoteItem>): List<Wallpaper> {
        val fileRegex = Regex("^([A-Za-z]+\\d+?)(\\d)\\.[A-Za-z0-9]+$")
        val groups = LinkedHashMap<String, MutableMap<Int, String>>()
        val groupCategory = HashMap<String, String?>()

        for (item in items) {
            val filename = item.filename ?: continue
            val url = item.url ?: continue
            val match = fileRegex.find(filename) ?: continue
            val groupKey = match.groupValues[1]
            val layer = match.groupValues[2].toIntOrNull() ?: continue
            groups.getOrPut(groupKey) { sortedMapOf() }[layer] = url
            groupCategory.putIfAbsent(groupKey, item.category)
        }

        return groups.entries.mapNotNull { (key, layers) ->
            val l1 = layers[1]
            val l2 = layers[2]
            val l3 = layers[3]
            val layerUris = listOfNotNull(l1, l2).ifEmpty { listOfNotNull(l3) }
            if (layerUris.isEmpty()) return@mapNotNull null
            val thumb = l3 ?: l1 ?: layerUris.first()
            Wallpaper(
                id = "remote_${key.lowercase()}",
                title = prettyTitle(key),
                category = WallpaperCategory.fromKey(groupCategory[key] ?: ""),
                thumbUri = thumb,
                layerUris = layerUris
            )
        }
    }

    private fun prettyTitle(groupKey: String): String {
        val m = Regex("([A-Za-z]+)(\\d+)").find(groupKey) ?: return groupKey
        val name = m.groupValues[1].replaceFirstChar { it.uppercase() }
        return "$name ${m.groupValues[2]}"
    }
}

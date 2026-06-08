package com.galaxywall.app.data.remote

import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches parallax items from the DataWallpaper API and groups their layers into [Wallpaper]s.
 *
 * Files follow `<Category><GroupId><LayerId>.{png|jpg}` (e.g. Anime11.png). Items sharing a group
 * key (e.g. Anime11/Anime12/Anime13 -> "Anime1") are merged into one wallpaper. Layer 1 is the
 * bottom layer (moves with the tilt) and layer 2 the top layer (stays still). Layer 3 is unused as
 * a render layer and only doubles as the grid thumbnail.
 */
@Singleton
class RemoteWallpaperSource @Inject constructor(
    private val api: WallpaperApi
) {
    suspend fun loadParallax(): List<Wallpaper> =
        groupItems(api.listParallax(limit = 200).data)

    /**
     * Every settable wallpaper from the API: parallax (multi-layer), static images and videos,
     * across all categories. The three endpoints are fetched concurrently.
     */
    suspend fun loadAll(): List<Wallpaper> = coroutineScope {
        val parallaxDef = async { runCatching { groupItems(api.listParallax(limit = 200).data) }.getOrDefault(emptyList()) }
        val imagesDef = async { runCatching { api.listImages(limit = 200).data.mapNotNull { toImage(it) } }.getOrDefault(emptyList()) }
        val videosDef = async { runCatching { api.listVideos(limit = 200).data.mapNotNull { toVideo(it) } }.getOrDefault(emptyList()) }
        // Exclude the fish and trending categories everywhere.
        (parallaxDef.await() + imagesDef.await() + videosDef.await())
            .filter { it.category !in EXCLUDED_CATEGORIES }
    }

    private fun toImage(item: RemoteItem): Wallpaper? {
        val url = item.url ?: return null
        return Wallpaper(
            id = item.id ?: "img_${item.filename ?: url}",
            title = item.title ?: prettyTitle(item.filename ?: "Image"),
            category = WallpaperCategory.fromKey(item.category ?: ""),
            thumbUri = item.thumbnailUrl ?: url,
            type = ContentType.IMAGE,
            sourceUrl = url
        )
    }

    private fun toVideo(item: RemoteItem): Wallpaper? {
        val url = item.url ?: return null
        return Wallpaper(
            id = item.id ?: "vid_${item.filename ?: url}",
            title = item.title ?: prettyTitle(item.filename ?: "Video"),
            category = WallpaperCategory.fromKey(item.category ?: ""),
            thumbUri = item.thumbnailUrl ?: url,
            type = ContentType.VIDEO,
            sourceUrl = url
        )
    }

    private fun groupItems(items: List<RemoteItem>): List<Wallpaper> {
        val fileRegex = Regex("^([A-Za-z]+\\d+?)(\\d)\\.[A-Za-z0-9]+$")
        val groups = LinkedHashMap<String, MutableMap<Int, String>>()
        val groupCategory = HashMap<String, String?>()

        for (item in items) {
            val filename = item.filename ?: continue
            val url = item.url ?: continue
            val match = fileRegex.find(filename) ?: continue
            // Normalize the group key to lower case so case-mismatched filenames (e.g. Sport31 /
            // sport32 / Sport33) still merge into ONE group. Otherwise a single odd-cased layer
            // splits off into its own group and the main group loses a layer, dropping the item to
            // a static (non-parallax) fallback.
            val groupKey = match.groupValues[1].lowercase()
            val layer = match.groupValues[2].toIntOrNull() ?: continue
            groups.getOrPut(groupKey) { sortedMapOf() }[layer] = url
            groupCategory.putIfAbsent(groupKey, item.category)
        }

        return groups.entries.mapNotNull { (key, layers) ->
            val l1 = layers[1]
            val l2 = layers[2]
            val l3 = layers[3]
            // bottom (layer 1) moves with the tilt, top (layer 2) stays still. If a group is missing
            // a layer, fall back to the pre-composed image (layer 3) so the item still shows fully
            // (complete, just static) instead of rendering an incomplete set.
            val layerUris = when {
                l1 != null && l2 != null -> listOf(l1, l2)
                l3 != null -> listOf(l3)
                else -> listOfNotNull(l1, l2)
            }
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

    private companion object {
        val EXCLUDED_CATEGORIES = setOf(WallpaperCategory.FISH, WallpaperCategory.TRENDING)
    }
}

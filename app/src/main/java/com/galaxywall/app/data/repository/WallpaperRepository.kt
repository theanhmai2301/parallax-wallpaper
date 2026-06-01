package com.galaxywall.app.data.repository

import com.galaxywall.app.data.local.FavoriteEntity
import com.galaxywall.app.data.local.RecentEntity
import com.galaxywall.app.data.local.WallpaperDao
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.remote.RemoteWallpaperSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperRepository @Inject constructor(
    private val remote: RemoteWallpaperSource,
    private val dao: WallpaperDao
) {
    private val mutex = Mutex()

    @Volatile
    private var cache: List<Wallpaper>? = null

    /** All settable content (parallax + images + videos) from the API. Empty result is not cached,
     *  so it retries (Render cold start). */
    private suspend fun loadCatalog(): List<Wallpaper> {
        cache?.let { return it }
        return mutex.withLock {
            cache ?: run {
                val list = runCatching { remote.loadAll() }.getOrDefault(emptyList())
                if (list.isNotEmpty()) cache = list
                list
            }
        }
    }

    fun invalidate() {
        cache = null
    }

    private fun catalogFlow(): Flow<List<Wallpaper>> = flow { emit(loadCatalog()) }

    fun observeWallpapers(type: ContentType?, query: String): Flow<List<Wallpaper>> =
        combine(catalogFlow(), dao.observeFavoriteIds()) { all, favIds ->
            val favSet = favIds.toHashSet()
            all.asSequence()
                .filter { type == null || it.type == type }
                .filter { matchesQuery(it, query) }
                .map { it.copy(isFavorite = favSet.contains(it.id)) }
                .toList()
        }

    fun observeFavorites(): Flow<List<Wallpaper>> =
        combine(catalogFlow(), dao.observeFavoriteIds()) { all, favIds ->
            val order = favIds.withIndex().associate { (i, id) -> id to i }
            all.filter { order.containsKey(it.id) }
                .map { it.copy(isFavorite = true) }
                .sortedBy { order[it.id] ?: Int.MAX_VALUE }
        }

    fun isFavorite(id: String): Flow<Boolean> = dao.isFavorite(id)

    suspend fun getById(id: String): Wallpaper? = loadCatalog().firstOrNull { it.id == id }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        if (favorite) dao.addFavorite(FavoriteEntity(id)) else dao.removeFavorite(id)
    }

    suspend fun markRecent(id: String) {
        dao.addRecent(RecentEntity(id))
    }

    private fun matchesQuery(wallpaper: Wallpaper, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return wallpaper.title.lowercase().contains(q) ||
            wallpaper.category.label.lowercase().contains(q) ||
            wallpaper.category.key.contains(q)
    }
}

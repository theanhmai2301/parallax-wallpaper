package com.galaxywall.app.data.repository

import com.galaxywall.app.data.local.FavoriteEntity
import com.galaxywall.app.data.local.RecentEntity
import com.galaxywall.app.data.local.WallpaperDao
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.model.WallpaperCategory
import com.galaxywall.app.data.remote.RemoteWallpaperSource
import kotlinx.coroutines.delay
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

    @Volatile
    private var feedCache: List<Wallpaper>? = null

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
        feedCache = null
    }

    /**
     * Emits the catalog, retrying while it comes back empty (no network yet / Render free-tier cold
     * start can take tens of seconds). Until the first non-empty result it emits nothing, so the
     * screen stays on its loading shimmer and then fills automatically — instead of flashing an empty
     * grid that never recovers until the user switches tab/chip. Gives up after [MAX_CATALOG_RETRIES]
     * and emits empty so the empty state can show.
     */
    private fun catalogFlow(): Flow<List<Wallpaper>> = flow {
        var attempt = 0
        while (true) {
            val list = loadCatalog()
            if (list.isNotEmpty() || attempt >= MAX_CATALOG_RETRIES) {
                emit(list)
                return@flow
            }
            attempt++
            delay(CATALOG_RETRY_DELAY_MS * attempt)
        }
    }

    fun observeWallpapers(
        type: ContentType?,
        category: WallpaperCategory?,
        query: String
    ): Flow<List<Wallpaper>> =
        combine(catalogFlow(), dao.observeFavoriteIds()) { all, favIds ->
            val favSet = favIds.toHashSet()
            all.asSequence()
                .filter { type == null || it.type == type }
                .filter { category == null || it.category == category }
                .filter { matchesQuery(it, query) }
                .map { it.copy(isFavorite = favSet.contains(it.id)) }
                .toList()
        }

    /**
     * Home feed: a curated random mix — a few items per category — built once and kept stable for
     * the session so it doesn't reshuffle on every favorite change. Small + uses thumbnails so fast
     * scrolling stays smooth. Rebuilt on [invalidate] (pull-to-refresh).
     */
    fun observeHomeFeed(): Flow<List<Wallpaper>> =
        combine(catalogFlow(), dao.observeFavoriteIds()) { all, favIds ->
            val favSet = favIds.toHashSet()
            val feed = feedCache ?: buildFeed(all).also { feedCache = it }
            feed.map { it.copy(isFavorite = favSet.contains(it.id)) }
        }

    private fun buildFeed(all: List<Wallpaper>): List<Wallpaper> =
        all.groupBy { it.category }
            .values
            .flatMap { items -> items.shuffled().take(FEED_PER_CATEGORY) }
            .shuffled()

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

    private companion object {
        const val FEED_PER_CATEGORY = 4
        // Up to ~1.5+3+4.5+...+12 ≈ 54s of retries, covering a cold backend start.
        const val MAX_CATALOG_RETRIES = 8
        const val CATALOG_RETRY_DELAY_MS = 1500L
    }
}

package com.galaxywall.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperDao {

    @Query("SELECT wallpaperId FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE wallpaperId = :id)")
    fun isFavorite(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE wallpaperId = :id")
    suspend fun removeFavorite(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(entity: RecentEntity)

    @Query("SELECT wallpaperId FROM recents ORDER BY viewedAt DESC LIMIT :limit")
    fun observeRecentIds(limit: Int): Flow<List<String>>
}

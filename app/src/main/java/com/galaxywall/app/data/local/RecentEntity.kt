package com.galaxywall.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey val wallpaperId: String,
    val viewedAt: Long = System.currentTimeMillis()
)

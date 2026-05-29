package com.galaxywall.app.data.remote

/** Item shape returned by /api/videos, /api/images and /api/parallax (DataWallpaper server). */
data class RemoteItem(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val category: String? = null,
    val tag: String? = null,
    val filename: String? = null,
    val url: String? = null,
    val thumbnailUrl: String? = null,
    val size: Long? = null,
    val mtime: String? = null
)

data class RemoteMeta(
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val count: Int = 0
)

data class RemoteListResponse(
    val data: List<RemoteItem> = emptyList(),
    val meta: RemoteMeta? = null
)

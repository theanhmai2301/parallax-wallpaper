package com.galaxywall.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface WallpaperApi {

    @GET("api/parallax")
    suspend fun listParallax(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0
    ): RemoteListResponse

    @GET("api/images")
    suspend fun listImages(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0
    ): RemoteListResponse

    @GET("api/videos")
    suspend fun listVideos(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0
    ): RemoteListResponse
}

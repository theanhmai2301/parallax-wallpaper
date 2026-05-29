package com.galaxywall.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size

/**
 * Loads bitmaps through Coil from remote image URLs ("https://...") and content URIs. Returns
 * software bitmaps (hardware disabled) so they can be drawn on a Canvas and used with
 * WallpaperManager.
 */
object BitmapLoader {

    suspend fun load(context: Context, uri: String, size: Size = Size.ORIGINAL): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .size(size)
            .build()
        val drawable = context.imageLoader.execute(request).drawable ?: return null
        return (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap()
    }
}

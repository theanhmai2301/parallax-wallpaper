package com.galaxywall.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.galaxywall.app.util.BitmapLoader
import coil.size.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperApplier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    enum class Target { HOME, LOCK, BOTH }

    suspend fun apply(bitmap: Bitmap, target: Target): Unit = withContext(Dispatchers.IO) {
        val manager = WallpaperManager.getInstance(context)
        val flags = when (target) {
            Target.HOME -> WallpaperManager.FLAG_SYSTEM
            Target.LOCK -> WallpaperManager.FLAG_LOCK
            Target.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        }
        manager.setBitmap(bitmap, null, true, flags)
    }

    /** Loads a static image from [url] and applies it. Returns true on success. */
    suspend fun applyFromUrl(url: String, target: Target): Boolean = withContext(Dispatchers.IO) {
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported) return@withContext false
        // Decode no larger than the device's desired wallpaper size. Loading the full-resolution
        // source as an ARGB_8888 bitmap can OOM (and is slow) on low-RAM devices; capping the size
        // keeps the apply path safe and fast across all devices while WallpaperManager still scales.
        val metrics = context.resources.displayMetrics
        val targetWidth = manager.desiredMinimumWidth.takeIf { it > 0 } ?: metrics.widthPixels
        val targetHeight = manager.desiredMinimumHeight.takeIf { it > 0 } ?: metrics.heightPixels
        val bitmap = BitmapLoader.load(context, url, Size(targetWidth, targetHeight))
            ?: return@withContext false
        runCatching { apply(bitmap, target) }.isSuccess
    }
}

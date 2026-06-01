package com.galaxywall.app.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Persists which video URL the [VideoWallpaperService] should play and builds the system
 *  "change live wallpaper" intent for it. */
object VideoWallpaperController {

    private const val PREFS = "video_wallpaper"
    const val KEY_URL = "url"

    fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setVideo(context: Context, url: String) {
        prefs(context).edit().putString(KEY_URL, url).apply()
    }

    fun getVideo(context: Context): String? = prefs(context).getString(KEY_URL, null)

    fun changeIntent(context: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, VideoWallpaperService::class.java)
        )
}

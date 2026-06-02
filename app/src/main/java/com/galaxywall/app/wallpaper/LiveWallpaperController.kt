package com.galaxywall.app.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Persists what the live wallpaper should render: the ordered layer URIs (bottom -> top), the
 * parallax depth and the overlay effect. Builds the system "change live wallpaper" intent.
 */
object LiveWallpaperController {

    private const val PREFS = "live_wallpaper"
    private const val KEY_LAYERS = "layers"
    private const val KEY_DEPTH = "depth"
    private const val KEY_EFFECT = "effect"
    private const val KEY_FALLBACK = "fallback"
    private const val LINE = "\n"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun setComposition(
        context: Context,
        layerUris: List<String>,
        depth: Float,
        effectOrdinal: Int,
        fallbackUri: String? = null
    ) {
        prefs(context).edit()
            .putString(KEY_LAYERS, layerUris.joinToString(LINE))
            .putFloat(KEY_DEPTH, depth)
            .putInt(KEY_EFFECT, effectOrdinal)
            .putString(KEY_FALLBACK, fallbackUri)
            .apply()
    }

    fun getLayerUris(context: Context): List<String> =
        prefs(context).getString(KEY_LAYERS, null)
            ?.split(LINE)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    /** The pre-composed full image, used when a layer fails to load so the wallpaper still shows. */
    fun getFallbackUri(context: Context): String? =
        prefs(context).getString(KEY_FALLBACK, null)?.takeIf { it.isNotBlank() }

    fun getDepth(context: Context): Float = prefs(context).getFloat(KEY_DEPTH, 0.5f)

    fun getEffectOrdinal(context: Context): Int = prefs(context).getInt(KEY_EFFECT, 0)

    fun changeIntent(context: Context): Intent =
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, ParallaxWallpaperService::class.java)
        )
}

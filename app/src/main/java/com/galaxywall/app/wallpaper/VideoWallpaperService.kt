package com.galaxywall.app.wallpaper

import android.content.SharedPreferences
import android.media.MediaPlayer
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder

/**
 * Live wallpaper that loops the MP4 saved by [VideoWallpaperController] (streamed from its URL),
 * muted, scaled to fill the surface. Playback pauses when not visible.
 *
 * Because picking a different video re-uses this same component, the system does NOT recreate the
 * engine/surface, so the URL is also watched via a preferences listener (and re-checked when the
 * wallpaper becomes visible) to swap to the new video without needing a fresh surface.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    private inner class VideoEngine : Engine() {

        private var player: MediaPlayer? = null
        private var prepared = false
        private var currentUrl: String? = null
        private var holder: SurfaceHolder? = null

        private val prefs = VideoWallpaperController.prefs(applicationContext)
        private val prefsListener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == VideoWallpaperController.KEY_URL) reloadIfChanged()
            }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        }

        override fun onSurfaceCreated(surfaceHolder: SurfaceHolder) {
            super.onSurfaceCreated(surfaceHolder)
            holder = surfaceHolder
            startPlayer(VideoWallpaperController.getVideo(applicationContext))
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                reloadIfChanged()
                if (prepared) runCatching { player?.start() }
            } else {
                runCatching { player?.pause() }
            }
        }

        /** Restarts playback if the saved URL no longer matches what is currently playing. */
        private fun reloadIfChanged() {
            val saved = VideoWallpaperController.getVideo(applicationContext)
            if (saved != null && saved != currentUrl) startPlayer(saved)
        }

        private fun startPlayer(url: String?) {
            url ?: return
            val surfaceHolder = holder ?: return
            currentUrl = url
            prepared = false
            releasePlayer()
            player = MediaPlayer().apply {
                setOnPreparedListener { mp ->
                    prepared = true
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    if (isVisible) runCatching { mp.start() }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("VideoWallpaper", "MediaPlayer error what=$what extra=$extra url=$url")
                    true
                }
                runCatching {
                    setDataSource(url)
                    setSurface(surfaceHolder.surface)
                    prepareAsync()
                }
            }
        }

        private fun releasePlayer() {
            runCatching { player?.release() }
            player = null
        }

        override fun onSurfaceDestroyed(surfaceHolder: SurfaceHolder) {
            super.onSurfaceDestroyed(surfaceHolder)
            prepared = false
            releasePlayer()
            holder = null
        }

        override fun onDestroy() {
            super.onDestroy()
            runCatching { prefs.unregisterOnSharedPreferenceChangeListener(prefsListener) }
            releasePlayer()
            holder = null
        }
    }
}

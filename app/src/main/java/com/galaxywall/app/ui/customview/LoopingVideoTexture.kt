package com.galaxywall.app.ui.customview

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView

/**
 * Plays a looping, muted video from a URL into a [TextureView] (streamed directly). A TextureView
 * (not a SurfaceView / VideoView) is used so it scales with page transforms and is clipped by
 * rounded card corners. Call [play] to (re)start with a URL and [stop] to release.
 */
class LoopingVideoTexture(private val textureView: TextureView) {

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var url: String? = null
    private var available = textureView.isAvailable

    init {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                available = true
                openIfReady()
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                available = false
                releasePlayer()
                return true
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    /** Starts (or restarts) playback of [videoUrl], looping and muted. */
    fun play(videoUrl: String) {
        if (url == videoUrl && player != null) return
        url = videoUrl
        openIfReady()
    }

    fun stop() {
        url = null
        releasePlayer()
    }

    private fun openIfReady() {
        val u = url ?: return
        if (!available) return
        val st = textureView.surfaceTexture ?: return
        releasePlayer()
        surface = Surface(st)
        player = MediaPlayer().apply {
            setOnPreparedListener {
                it.isLooping = true
                it.setVolume(0f, 0f)
                runCatching { it.start() }
            }
            setOnErrorListener { _, what, extra ->
                Log.e("LoopingVideoTexture", "MediaPlayer error what=$what extra=$extra url=$u")
                true
            }
            runCatching {
                setDataSource(u)
                setSurface(surface)
                prepareAsync()
            }
        }
    }

    private fun releasePlayer() {
        runCatching { player?.release() }
        player = null
        surface?.release()
        surface = null
    }
}

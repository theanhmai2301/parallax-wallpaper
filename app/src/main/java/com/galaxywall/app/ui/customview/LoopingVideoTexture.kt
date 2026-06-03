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
    private var prepared = false

    /** Whether the prepared player should be playing (play) or paused-after-buffering (preload). */
    private var shouldPlay = false

    /** Invoked once the video is buffered and ready to play (used to gate the "set wallpaper" CTA). */
    var onReady: (() -> Unit)? = null

    /** Invoked when the video fails to load/stream (e.g. offline). */
    var onError: (() -> Unit)? = null

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

    /** Starts (or resumes) playback of [videoUrl], looping and muted. If it was preloaded, the
     *  buffered player just starts — no fresh network open, so the swap is instant. */
    fun play(videoUrl: String) {
        shouldPlay = true
        if (url == videoUrl && player != null) {
            if (prepared) runCatching { player?.start() }
            return
        }
        url = videoUrl
        openIfReady()
    }

    /** Buffers [videoUrl] ahead of time but stays paused (used for the off-centre neighbour pages),
     *  so swiping onto the page resumes instantly instead of waiting for a fresh network prepare. */
    fun preload(videoUrl: String) {
        shouldPlay = false
        if (url == videoUrl && player != null) {
            // Already opened. Only pause if it is actually playing (this page just left the centre).
            // Calling pause() on a merely-PREPARED player is invalid and throws error (-38), which
            // pushes the player into an Error state so it can never start afterwards.
            runCatching { if (player?.isPlaying == true) player?.pause() }
            return
        }
        url = videoUrl
        openIfReady()
    }

    fun stop() {
        url = null
        shouldPlay = false
        releasePlayer()
    }

    private fun openIfReady() {
        val u = url ?: return
        if (!available) return
        val st = textureView.surfaceTexture ?: return
        releasePlayer()
        surface = Surface(st)
        prepared = false
        player = MediaPlayer().apply {
            setOnPreparedListener {
                prepared = true
                it.isLooping = true
                it.setVolume(0f, 0f)
                // Only auto-start when this page is the centred (playing) one; preloaded neighbours
                // stay paused at the buffered first frame.
                if (shouldPlay) runCatching { it.start() }
                onReady?.invoke()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("LoopingVideoTexture", "MediaPlayer error what=$what extra=$extra url=$u")
                onError?.invoke()
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
        prepared = false
        runCatching { player?.release() }
        player = null
        surface?.release()
        surface = null
    }
}

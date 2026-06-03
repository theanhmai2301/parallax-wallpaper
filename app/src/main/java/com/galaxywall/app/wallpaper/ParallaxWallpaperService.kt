package com.galaxywall.app.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import coil.size.Size
import com.galaxywall.app.data.local.SettingsManager
import com.galaxywall.app.sensors.ParallaxSensorManager
import com.galaxywall.app.ui.builder.OverlayEffect
import com.galaxywall.app.util.BitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max

/**
 * Live wallpaper that renders the saved layers (bottom -> top) with gyroscope parallax. The bottom
 * layer moves the most and fills the surface; the top layer stays still. [depth] over-scales the
 * moving layer (zooms it in) and the layer travels exactly that extra margin, so a larger depth
 * tilts further while always keeping the frame covered — no black edge ever shows. Layers are drawn
 * as-is (no background removal).
 */
class ParallaxWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = ParallaxEngine()

    private inner class ParallaxEngine : Engine(), Choreographer.FrameCallback {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val sensor = ParallaxSensorManager(applicationContext)
        private val settingsManager = SettingsManager(applicationContext)

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val matrix = Matrix()

        private var renderLayers: List<Bitmap> = emptyList()
        private var effectFilter: ColorFilter? = null
        private var depth = 0.5f

        private var targetX = 0f
        private var targetY = 0f
        private var curX = 0f
        private var curY = 0f
        private var pageOffset = 0.5f

        private var surfaceW = 0
        private var surfaceH = 0
        private var screenAspect = 0.5f
        private var loadedKey: String? = null
        private var visible = false
        private var parallaxEnabled = true

        private var lastX = Float.NaN
        private var lastY = Float.NaN
        private var lastPage = -1f
        private var redrawDirty = true

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val dm = applicationContext.resources.displayMetrics
            if (dm.widthPixels > 0 && dm.heightPixels > 0) {
                screenAspect = dm.widthPixels.toFloat() / dm.heightPixels
            }
            sensor.setListener { x, y ->
                if (parallaxEnabled) { targetX = x; targetY = y }
            }
            settingsManager.settings
                .onEach {
                    parallaxEnabled = it.parallaxEnabled
                    sensor.sensitivity = it.sensitivity
                    if (!parallaxEnabled) { targetX = 0f; targetY = 0f }
                    redrawDirty = true
                }
                .launchIn(scope)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceW = width
            surfaceH = height
            redrawDirty = true
            loadComposition()
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xStep: Float, yStep: Float,
            xPixels: Int, yPixels: Int
        ) {
            pageOffset = xOffset.coerceIn(0f, 1f)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                redrawDirty = true
                sensor.start()
                loadComposition()
                Choreographer.getInstance().postFrameCallback(this)
            } else {
                sensor.stop()
                Choreographer.getInstance().removeFrameCallback(this)
            }
        }

        private fun loadComposition() {
            if (surfaceW == 0 || surfaceH == 0) return
            val uris = LiveWallpaperController.getLayerUris(applicationContext)
            val depthVal = LiveWallpaperController.getDepth(applicationContext)
            val effectOrdinal = LiveWallpaperController.getEffectOrdinal(applicationContext)
            val key = uris.joinToString("/") + "#$depthVal#$effectOrdinal@$surfaceW"
            if (loadedKey == key && renderLayers.isNotEmpty()) return
            val fallbackUri = LiveWallpaperController.getFallbackUri(applicationContext)
            scope.launch {
                val size = Size(surfaceW, surfaceH)
                val bitmaps = withContext(Dispatchers.IO) {
                    val loaded = uris.map { BitmapLoader.load(applicationContext, it, size) }
                    val allLoaded = loaded.none { it == null }
                    // The top (last) layer must keep transparency; an opaque top hides everything below.
                    val topOpaque = loaded.lastOrNull()?.let { !it.hasAlpha() } ?: true
                    if (uris.size > 1 && (!allLoaded || topOpaque) && fallbackUri != null) {
                        // Broken layer stack -> show the pre-composed full image instead.
                        BitmapLoader.load(applicationContext, fallbackUri, size)?.let { listOf(it) }
                            ?: loaded.filterNotNull()
                    } else {
                        loaded.filterNotNull()
                    }
                }
                renderLayers = bitmaps
                depth = depthVal
                effectFilter = OverlayEffect.entries.getOrNull(effectOrdinal)?.colorFilter()
                paint.colorFilter = effectFilter
                loadedKey = key
                redrawDirty = true
                drawFrame()
            }
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!visible) return
            curX += (targetX - curX) * EASE
            curY += (targetY - curY) * EASE
            val changed = redrawDirty ||
                abs(curX - lastX) > REDRAW_EPS ||
                abs(curY - lastY) > REDRAW_EPS ||
                pageOffset != lastPage
            if (changed) {
                drawFrame()
                lastX = curX
                lastY = curY
                lastPage = pageOffset
                redrawDirty = false
            }
            Choreographer.getInstance().postFrameCallback(this)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) render(canvas)
            } finally {
                if (canvas != null) {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: IllegalArgumentException) { }
                }
            }
        }

        private fun render(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
            if (renderLayers.isEmpty()) return

            // COVER the surface with the foreground content (never FIT/letterbox):
            // the wallpaper surface can be taller than the screen aspect, and a
            // FIT box would leave the foreground short of the surface -> a gap at
            // the bottom for tall (e.g. anime portrait) images. Covering keeps the
            // foreground filling the whole surface, matching the in-app preview.
            val surfaceAspect = surfaceW.toFloat() / surfaceH
            val contentW: Float
            val contentH: Float
            if (surfaceAspect > screenAspect) {
                contentW = surfaceW.toFloat()
                contentH = contentW / screenAspect
            } else {
                contentH = surfaceH.toFloat()
                contentW = contentH * screenAspect
            }
            val offX = (surfaceW - contentW) * pageOffset
            val offY = (surfaceH - contentH) / 2f

            val n = renderLayers.size
            renderLayers.forEachIndexed { i, bitmap ->
                // Bottom layer (i=0) moves the most; top layer stays still.
                val factor = if (n <= 1) 0f else 1f - i.toFloat() / (n - 1)
                if (i == 0) {
                    drawBackground(canvas, bitmap, factor)
                } else {
                    val saveCount = canvas.save()
                    canvas.clipRect(offX, offY, offX + contentW, offY + contentH)
                    drawInContent(canvas, bitmap, factor, contentW, contentH, offX, offY)
                    canvas.restoreToCount(saveCount)
                }
            }
        }

        /** Bottom layer fills the whole surface (no black margins) and moves with the tilt. */
        private fun drawBackground(canvas: Canvas, bitmap: Bitmap, factor: Float) {
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()
            if (bw <= 0f || bh <= 0f) return
            val scale = max(surfaceW / bw, surfaceH / bh) * (1f + OVERSCALE_AMOUNT * depth * factor)
            val sw = bw * scale
            val sh = bh * scale
            val marginX = (sw - surfaceW) / 2f
            val marginY = (sh - surfaceH) / 2f
            val tx = (surfaceW - sw) / 2f + curX * marginX * factor
            val ty = (surfaceH - sh) / 2f + curY * marginY * factor
            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(tx, ty)
            canvas.drawBitmap(bitmap, matrix, paint)
        }

        private fun drawInContent(
            canvas: Canvas,
            bitmap: Bitmap,
            factor: Float,
            contentW: Float,
            contentH: Float,
            offX: Float,
            offY: Float
        ) {
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()
            if (bw <= 0f || bh <= 0f) return
            val scale = max(contentW / bw, contentH / bh) * (1f + OVERSCALE_AMOUNT * depth * factor)
            val sw = bw * scale
            val sh = bh * scale
            val marginX = (sw - contentW) / 2f
            val marginY = (sh - contentH) / 2f
            val tx = offX + (contentW - sw) / 2f + curX * marginX * factor
            val ty = offY + (contentH - sh) / 2f + curY * marginY * factor
            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(tx, ty)
            canvas.drawBitmap(bitmap, matrix, paint)
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
            sensor.stop()
            Choreographer.getInstance().removeFrameCallback(this)
            scope.cancel()
            renderLayers = emptyList()
        }
    }

    companion object {
        private const val EASE = 0.12f
        private const val REDRAW_EPS = 0.0008f
        private const val OVERSCALE_AMOUNT = 0.6f
    }
}

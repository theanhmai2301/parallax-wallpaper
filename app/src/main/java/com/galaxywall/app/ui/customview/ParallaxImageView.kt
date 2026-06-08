package com.galaxywall.app.ui.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * Renders a stack of layers (bottom -> top) and shifts them by the device tilt to create depth.
 * The bottom layer moves the most while the top layer stays still. [parallaxDepth] over-scales the
 * moving layer (zooms it in): a larger depth means more zoom and therefore more room to shift, so
 * raising it tilts further while the layer always keeps the frame covered — no black edge ever
 * shows. Motion is eased each frame on the Choreographer for a smooth 60fps feel.
 */
class ParallaxImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle), Choreographer.FrameCallback {

    data class LayerInput(val bitmap: Bitmap)

    private val layers = ArrayList<Bitmap>(3)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()

    private var targetX = 0f
    private var targetY = 0f
    private var curX = 0f
    private var curY = 0f

    /** 0f..1f — how far the foreground layers shift with the tilt (depth / tilt amount). */
    private var parallaxDepth = 0.5f

    /** Render content at this width/height aspect (e.g. the screen) so preview == final wallpaper. */
    var targetAspect: Float = 0f
        set(value) { field = value; invalidate() }

    var parallaxEnabled = true
        set(value) {
            field = value
            if (!value) { targetX = 0f; targetY = 0f }
        }

    /** Enable drag-to-parallax when the device has no usable motion sensor. */
    var touchParallaxEnabled = false

    private var running = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    fun setLayers(inputs: List<LayerInput>) {
        layers.clear()
        inputs.forEach { layers.add(it.bitmap) }
        invalidate()
    }

    fun setParallaxDepth(depth: Float) {
        parallaxDepth = depth.coerceIn(0f, 1f)
        invalidate()
    }

    /** Normalized tilt in [-1, 1] from [com.galaxywall.app.sensors.ParallaxSensorManager]. */
    fun setOffset(x: Float, y: Float) {
        if (!parallaxEnabled) return
        targetX = x.coerceIn(-1f, 1f)
        targetY = y.coerceIn(-1f, 1f)
    }

    fun setEffectColorFilter(filter: ColorFilter?) {
        paint.colorFilter = filter
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDetachedFromWindow() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        super.onDetachedFromWindow()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        curX += (targetX - curX) * EASE
        curY += (targetY - curY) * EASE
        if (abs(targetX - curX) > EPS || abs(targetY - curY) > EPS) invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        if (layers.isEmpty() || width == 0 || height == 0) return

        val contentW: Float
        val contentH: Float
        val offX: Float
        val offY: Float
        if (targetAspect > 0f) {
            val viewAspect = width.toFloat() / height
            if (viewAspect > targetAspect) {
                contentH = height.toFloat()
                contentW = contentH * targetAspect
            } else {
                contentW = width.toFloat()
                contentH = contentW / targetAspect
            }
            offX = (width - contentW) / 2f
            offY = (height - contentH) / 2f
        } else {
            contentW = width.toFloat()
            contentH = height.toFloat()
            offX = 0f
            offY = 0f
        }

        val saveCount = canvas.save()
        canvas.clipRect(offX, offY, offX + contentW, offY + contentH)
        val n = layers.size
        layers.forEachIndexed { i, bitmap ->
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()
            if (bw <= 0f || bh <= 0f) return@forEachIndexed
            // Bottom layer (i=0) moves the most; top layer stays still. A SINGLE layer (a
            // pre-composed full image, e.g. Anime9) gets factor 1 so the whole image still pans/
            // zooms with the tilt — otherwise it would sit dead still with no parallax at all.
            val factor = if (n <= 1) 1f else 1f - i.toFloat() / (n - 1)
            // Over-scale the moving layer by the depth, then let it travel exactly the extra margin
            // that over-scale created — so it shifts as far as possible while still covering the frame.
            val scale = max(contentW / bw, contentH / bh) * (1f + OVERSCALE_AMOUNT * parallaxDepth * factor)
            val sw = bw * scale
            val sh = bh * scale
            val marginX = (sw - contentW) / 2f
            val marginY = (sh - contentH) / 2f
            // Multiply by factor so the still (top) layer (factor 0) never drifts, even when its
            // cover-fit overflows the frame; only the moving layer travels within its margin.
            val tx = offX + (contentW - sw) / 2f + curX * marginX * factor
            val ty = offY + (contentH - sh) / 2f + curY * marginY * factor
            matrix.reset()
            matrix.postScale(scale, scale)
            matrix.postTranslate(tx, ty)
            canvas.drawBitmap(bitmap, matrix, paint)
        }
        canvas.restoreToCount(saveCount)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!touchParallaxEnabled || !parallaxEnabled) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val halfW = (width / 2f).coerceAtLeast(1f)
                val halfH = (height / 2f).coerceAtLeast(1f)
                targetX = (targetX - (event.x - lastTouchX) / halfW).coerceIn(-1f, 1f)
                targetY = (targetY - (event.y - lastTouchY) / halfH).coerceIn(-1f, 1f)
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    companion object {
        private const val EASE = 0.12f
        private const val EPS = 0.0005f
        private const val OVERSCALE_AMOUNT = 0.6f
    }
}

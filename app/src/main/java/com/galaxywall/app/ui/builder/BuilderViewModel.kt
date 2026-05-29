package com.galaxywall.app.ui.builder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.R
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.data.repository.WallpaperRepository
import com.galaxywall.app.ui.customview.ParallaxImageView
import com.galaxywall.app.util.BitmapLoader
import com.galaxywall.app.wallpaper.LiveWallpaperController
import com.galaxywall.app.wallpaper.WallpaperApplier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max

/**
 * Shared, activity-scoped state for the wallpaper builder flow:
 * Preview -> Edit (layers + depth) -> Overlay (effect) -> Result -> apply.
 *
 * Layers are composited as-is (no background removal). The bottom layer is the background and moves
 * with the tilt; the top layer stays fixed. [parallaxDepth] controls the tilt intensity.
 */
@HiltViewModel
class BuilderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WallpaperRepository,
    private val applier: WallpaperApplier
) : ViewModel() {

    data class LayerState(
        val bottom: String? = null,
        val middle: String? = null,
        val top: String? = null
    ) {
        fun ordered(): List<String> = listOfNotNull(bottom, middle, top)
    }

    sealed interface Event {
        data class Applied(val success: Boolean) : Event
    }

    var previewList: List<Wallpaper> = emptyList()
        private set

    private val _index = MutableStateFlow(0)
    val index = _index.asStateFlow()

    private val _layers = MutableStateFlow(LayerState())
    val layers = _layers.asStateFlow()

    private val _effect = MutableStateFlow(OverlayEffect.NONE)
    val effect = _effect.asStateFlow()

    private val _parallaxDepth = MutableStateFlow(0.5f)
    val parallaxDepth = _parallaxDepth.asStateFlow()

    private val _working = MutableStateFlow<String?>(null)
    val working = _working.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun startFrom(list: List<Wallpaper>, startIndex: Int) {
        previewList = list
        _index.value = startIndex.coerceIn(0, max(0, list.size - 1))
        _layers.value = LayerState()
        _effect.value = OverlayEffect.NONE
        _parallaxDepth.value = 0.5f
    }

    fun startBlank() {
        previewList = emptyList()
        _index.value = 0
        _layers.value = LayerState()
        _effect.value = OverlayEffect.NONE
        _parallaxDepth.value = 0.5f
    }

    fun setIndex(i: Int) {
        if (previewList.isEmpty()) return
        _index.value = i.coerceIn(0, previewList.size - 1)
    }

    /** Pre-fills the layer slots from the currently previewed wallpaper. */
    fun prepareLayersFromCurrent() {
        val wp = previewList.getOrNull(_index.value) ?: return
        _layers.value = LayerState(
            bottom = wp.layerUris.getOrNull(0),
            middle = wp.layerUris.getOrNull(1),
            top = wp.layerUris.getOrNull(2)
        )
    }

    fun setEffect(value: OverlayEffect) {
        _effect.value = value
    }

    fun setParallaxDepth(value: Float) {
        _parallaxDepth.value = value.coerceIn(0f, 1f)
    }

    fun currentLayerUris(): List<String> = _layers.value.ordered()

    /** Loads the ordered layer bitmaps as-is (bottom -> top), no background removal. */
    suspend fun loadRenderLayers(): List<ParallaxImageView.LayerInput> = withContext(Dispatchers.IO) {
        currentLayerUris().mapNotNull { uri ->
            BitmapLoader.load(context, uri)?.let { ParallaxImageView.LayerInput(it) }
        }
    }

    /** Saves the composition (layers + depth + effect) for the live wallpaper service. */
    fun saveLiveComposition() {
        LiveWallpaperController.setComposition(
            context,
            currentLayerUris(),
            _parallaxDepth.value,
            _effect.value.ordinal
        )
    }

    /** Static fallback used when the device has no live-wallpaper picker. */
    fun setWallpaper(target: WallpaperApplier.Target) {
        viewModelScope.launch {
            _working.value = context.getString(R.string.applying)
            val bitmap = withContext(Dispatchers.IO) { buildComposite() }
            val success = bitmap != null && runCatching { applier.apply(bitmap, target) }.isSuccess
            _working.value = null
            _events.emit(Event.Applied(success))
        }
    }

    private suspend fun buildComposite(): Bitmap? {
        val bitmaps = withContext(Dispatchers.IO) {
            currentLayerUris().mapNotNull { BitmapLoader.load(context, it) }
        }
        if (bitmaps.isEmpty()) return null
        val base = bitmaps.maxByOrNull { it.width.toLong() * it.height }!!
        val w = base.width
        val h = base.height
        val raw = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(raw)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        bitmaps.forEach { drawCover(canvas, it, w, h, paint) }

        val filter = _effect.value.colorFilter() ?: return raw
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(
            raw, 0f, 0f,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = filter }
        )
        raw.recycle()
        return out
    }

    private fun drawCover(canvas: Canvas, bitmap: Bitmap, w: Int, h: Int, paint: Paint) {
        val scale = max(w.toFloat() / bitmap.width, h.toFloat() / bitmap.height)
        val dx = (w - bitmap.width * scale) / 2f
        val dy = (h - bitmap.height * scale) / 2f
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }
}

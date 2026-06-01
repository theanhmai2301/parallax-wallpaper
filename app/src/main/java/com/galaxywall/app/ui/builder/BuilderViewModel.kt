package com.galaxywall.app.ui.builder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxywall.app.R
import com.galaxywall.app.data.model.ContentType
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.max

/**
 * Shared, activity-scoped state for the wallpaper builder flow:
 * Preview -> Edit (layers + depth) -> Result -> apply.
 *
 * Layers are composited as-is (no background removal). The bottom layer moves with the tilt; the
 * top layer stays still. [parallaxDepth] controls the tilt amount. There are two layers only
 * (bottom + top); a DIY user can pick either from the gallery.
 */
@HiltViewModel
class BuilderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WallpaperRepository,
    private val applier: WallpaperApplier
) : ViewModel() {

    enum class Slot { BOTTOM, TOP }

    data class LayerState(
        val bottom: String? = null,
        val top: String? = null
    ) {
        fun ordered(): List<String> = listOfNotNull(bottom, top)
    }

    sealed interface Event {
        data class Applied(val success: Boolean) : Event
    }

    var previewList: List<Wallpaper> = emptyList()
        private set

    /** True only when the builder was opened in DIY mode; gallery picking is allowed then. */
    var isDiy: Boolean = false
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
        isDiy = false
        previewList = list
        _index.value = startIndex.coerceIn(0, max(0, list.size - 1))
        _layers.value = LayerState()
        _effect.value = OverlayEffect.NONE
        _parallaxDepth.value = 0.5f
    }

    fun startBlank() {
        isDiy = true
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
        val uris = wp.layerUris
        // bottom = the moving layer, top = the still layer.
        _layers.value = LayerState(
            bottom = uris.firstOrNull(),
            top = uris.getOrNull(1)
        )
    }

    /** Assigns a gallery image to a layer slot (DIY). The image is copied into app storage so the
     *  live wallpaper service can still read it later, then exposed as a file:// URI. */
    fun pickLayer(slot: Slot, uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { copyToInternal(uri, slot.name.lowercase()) }
                ?: return@launch
            _layers.update { state ->
                when (slot) {
                    Slot.BOTTOM -> state.copy(bottom = path)
                    Slot.TOP -> state.copy(top = path)
                }
            }
        }
    }

    private fun copyToInternal(uri: Uri, name: String): String? = try {
        context.filesDir.listFiles { f -> f.name.startsWith("diy_${name}_") }?.forEach { it.delete() }
        val file = File(context.filesDir, "diy_${name}_${System.currentTimeMillis()}.img")
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        "file://${file.absolutePath}"
    } catch (e: Exception) {
        null
    }

    fun setEffect(value: OverlayEffect) {
        _effect.value = value
    }

    fun setParallaxDepth(value: Float) {
        _parallaxDepth.value = value.coerceIn(0f, 1f)
    }

    fun currentLayerUris(): List<String> = _layers.value.ordered()

    fun currentWallpaper(): Wallpaper? = previewList.getOrNull(_index.value)

    fun currentType(): ContentType = currentWallpaper()?.type ?: ContentType.PARALLAX

    /**
     * URIs to render in the preview. Picked/edited layers win (parallax + DIY); otherwise the item
     * is shown by type: parallax -> its layers, image/video -> a single static image (full image for
     * a photo, the thumbnail for a video).
     */
    private fun renderUris(): List<String> {
        val edited = currentLayerUris()
        if (edited.isNotEmpty()) return edited
        val wp = currentWallpaper() ?: return emptyList()
        return when (wp.type) {
            ContentType.PARALLAX -> wp.layerUris
            // A video's sourceUrl is an MP4 (not decodable as a bitmap) — preview its thumbnail.
            ContentType.VIDEO -> listOfNotNull(wp.thumbUri)
            else -> listOfNotNull(wp.sourceUrl ?: wp.thumbUri)
        }
    }

    /** Loads the ordered render bitmaps as-is (bottom -> top), no background removal. */
    suspend fun loadRenderLayers(): List<ParallaxImageView.LayerInput> = withContext(Dispatchers.IO) {
        renderUris().mapNotNull { uri ->
            BitmapLoader.load(context, uri)?.let { ParallaxImageView.LayerInput(it) }
        }
    }

    /** Applies the current static image (IMAGE type) directly with WallpaperManager. */
    fun applyStaticImage(target: WallpaperApplier.Target) {
        viewModelScope.launch {
            val url = currentWallpaper()?.sourceUrl
            if (url == null) {
                _events.emit(Event.Applied(false))
                return@launch
            }
            _working.value = context.getString(R.string.applying)
            val ok = applier.applyFromUrl(url, target)
            _working.value = null
            _events.emit(Event.Applied(ok))
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

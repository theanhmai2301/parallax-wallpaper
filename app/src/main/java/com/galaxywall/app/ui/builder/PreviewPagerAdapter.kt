package com.galaxywall.app.ui.builder

import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.size.Size
import com.galaxywall.app.R
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.ui.customview.LoopingVideoTexture
import com.galaxywall.app.ui.customview.ParallaxImageView
import com.galaxywall.app.util.BitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Carousel pages that render each wallpaper with [ParallaxImageView]: parallax items show the live
 * tilt (their layers), while images/videos show a single static layer (the full photo or the video
 * thumbnail). The device tilt is forwarded from the fragment.
 */
class PreviewPagerAdapter(
    private val items: List<Wallpaper>,
    private val scope: CoroutineScope,
    private val loadSize: Size,
    private val sensorAvailable: Boolean,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PreviewPagerAdapter.PageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_page, parent, false)
        return PageVH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        val wp = items[position]
        val isParallax = wp.type == ContentType.PARALLAX
        // The frame fills the page height; the image covers it (targetAspect = 0).
        holder.preview.targetAspect = 0f
        holder.preview.parallaxEnabled = isParallax
        holder.preview.touchParallaxEnabled = isParallax && !sensorAvailable
        holder.preview.setOnClickListener { onClick(holder.bindingAdapterPosition) }
        holder.video.setOnClickListener { onClick(holder.bindingAdapterPosition) }

        // Type tag (icon + label) so the user knows parallax / photo / video.
        val (tagIcon, tagLabel) = when (wp.type) {
            ContentType.PARALLAX -> R.drawable.ic_layers to "Parallax"
            ContentType.VIDEO -> R.drawable.ic_live to "Live"
            ContentType.IMAGE -> R.drawable.ic_image to "Photo"
        }
        holder.tag.text = tagLabel
        val tagSize = (14 * holder.tag.resources.displayMetrics.density).toInt()
        val tagDrawable = ContextCompat.getDrawable(holder.tag.context, tagIcon)?.apply {
            setBounds(0, 0, tagSize, tagSize)
            setTint(ContextCompat.getColor(holder.tag.context, R.color.white))
        }
        holder.tag.setCompoundDrawablesRelative(tagDrawable, null, null, null)

        // Video pages get a TextureView on top (the fragment starts the centered page's playback);
        // the thumbnail underneath acts as a poster until the first frame draws.
        if (wp.type == ContentType.VIDEO) {
            holder.videoUrl = wp.sourceUrl
            holder.video.isVisible = true
        } else {
            holder.videoUrl = null
            holder.stopVideo()
            holder.video.isVisible = false
        }

        val uris = when {
            isParallax && wp.layerUris.isNotEmpty() -> wp.layerUris
            // A video's sourceUrl is an MP4 (Coil can't decode it) — show its image thumbnail.
            wp.type == ContentType.VIDEO -> listOf(wp.thumbUri)
            else -> listOfNotNull(wp.sourceUrl ?: wp.thumbUri)
        }

        holder.job?.cancel()
        val ctx = holder.preview.context
        holder.job = scope.launch {
            val inputs = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    BitmapLoader.load(ctx, uri, loadSize)?.let { ParallaxImageView.LayerInput(it) }
                }
            }
            holder.preview.setLayers(inputs)
        }
    }

    override fun onViewRecycled(holder: PageVH) {
        holder.job?.cancel()
        holder.job = null
        holder.stopVideo()
    }

    class PageVH(view: View) : RecyclerView.ViewHolder(view) {
        val preview: ParallaxImageView = view.findViewById(R.id.pagePreview)
        val video: TextureView = view.findViewById(R.id.pageVideo)
        val tag: TextView = view.findViewById(R.id.pageTag)
        private val videoPlayer = LoopingVideoTexture(video)
        var videoUrl: String? = null
        var job: Job? = null

        fun playVideo() {
            videoUrl?.let { videoPlayer.play(it) }
        }

        /** Buffer this page's video ahead of time (paused) so swiping onto it plays instantly. */
        fun preloadVideo() {
            videoUrl?.let { videoPlayer.preload(it) }
        }

        fun stopVideo() {
            videoPlayer.stop()
        }
    }
}

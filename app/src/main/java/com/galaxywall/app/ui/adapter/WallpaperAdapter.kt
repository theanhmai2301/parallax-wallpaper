package com.galaxywall.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import coil.load
import coil.size.Size
import com.galaxywall.app.R
import com.galaxywall.app.data.model.ContentType
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.databinding.ItemWallpaperBinding
import com.galaxywall.app.ui.customview.ParallaxImageView
import com.galaxywall.app.util.BitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WallpaperAdapter(
    private val onClick: (Wallpaper, View) -> Unit,
    private val onFavorite: (Wallpaper) -> Unit,
    private val scope: CoroutineScope? = null,
    private val loadSize: Size = Size.ORIGINAL,
    private val animateParallax: Boolean = false
) : ListAdapter<Wallpaper, WallpaperAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWallpaperBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position)
    }

    override fun onViewRecycled(holder: VH) {
        holder.clearParallax()
    }

    inner class VH(private val binding: ItemWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var parallaxJob: Job? = null

        fun clearParallax() {
            parallaxJob?.cancel()
            parallaxJob = null
            binding.thumbParallax.setLayers(emptyList())
            binding.thumbParallax.isVisible = false
        }

        fun bind(item: Wallpaper, position: Int) {
            binding.title.text = item.title
            binding.categoryTag.text = item.category.label
            binding.resolution.text = item.resolutionLabel

            (binding.thumb.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
                RATIOS[position % RATIOS.size]
            binding.thumb.requestLayout()
            binding.thumb.transitionName = "thumb_${item.id}"

            binding.thumb.load(item.thumbUri) {
                crossfade(220)
                placeholder(R.drawable.shape_shimmer)
                error(R.drawable.ic_image_broken)
            }

            binding.playBadge.isVisible = item.type == ContentType.VIDEO

            // Parallax items animate with the device tilt right in the grid (Home only).
            parallaxJob?.cancel()
            val s = scope
            val animate = animateParallax && s != null &&
                item.type == ContentType.PARALLAX && item.layerUris.isNotEmpty()
            binding.thumbParallax.isVisible = animate
            if (animate && s != null) {
                binding.thumbParallax.targetAspect = 0f
                binding.thumbParallax.parallaxEnabled = true
                val ctx = binding.thumbParallax.context
                parallaxJob = s.launch {
                    val inputs = withContext(Dispatchers.IO) {
                        item.layerUris.mapNotNull { uri ->
                            BitmapLoader.load(ctx, uri, loadSize)
                                ?.let { ParallaxImageView.LayerInput(it) }
                        }
                    }
                    binding.thumbParallax.setLayers(inputs)
                }
            } else {
                binding.thumbParallax.setLayers(emptyList())
            }

            val favColor = if (item.isFavorite) R.color.favorite_red else R.color.white
            binding.favoriteButton.setImageResource(
                if (item.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            binding.favoriteButton.setColorFilter(
                ContextCompat.getColor(binding.root.context, favColor)
            )

            binding.card.setOnClickListener { onClick(item, binding.thumb) }
            binding.favoriteButton.setOnClickListener { onFavorite(item) }
        }
    }

    companion object {
        private val RATIOS = listOf("H,3:4", "H,2:3", "H,9:16", "H,4:5")

        private val DIFF = object : DiffUtil.ItemCallback<Wallpaper>() {
            override fun areItemsTheSame(oldItem: Wallpaper, newItem: Wallpaper) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Wallpaper, newItem: Wallpaper) =
                oldItem == newItem
        }
    }
}

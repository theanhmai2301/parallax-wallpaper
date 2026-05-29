package com.galaxywall.app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.galaxywall.app.R
import com.galaxywall.app.data.model.Wallpaper
import com.galaxywall.app.databinding.ItemWallpaperBinding

class WallpaperAdapter(
    private val onClick: (Wallpaper, View) -> Unit,
    private val onFavorite: (Wallpaper) -> Unit
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

    inner class VH(private val binding: ItemWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root) {

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

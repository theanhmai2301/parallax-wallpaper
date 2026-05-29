package com.galaxywall.app.ui.builder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.galaxywall.app.R
import com.galaxywall.app.data.model.Wallpaper

class PreviewPagerAdapter(
    private val items: List<Wallpaper>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PreviewPagerAdapter.PageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_page, parent, false)
        return PageVH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        holder.image.load(items[position].thumbUri) {
            crossfade(220)
            placeholder(R.drawable.shape_shimmer)
            error(R.drawable.ic_image_broken)
        }
        holder.image.setOnClickListener { onClick(holder.bindingAdapterPosition) }
    }

    class PageVH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.pageImage)
    }
}

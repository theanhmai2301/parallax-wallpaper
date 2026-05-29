package com.galaxywall.app.ui.builder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.galaxywall.app.R
import com.galaxywall.app.util.dp
import com.google.android.material.card.MaterialCardView

class EffectAdapter(
    private val previewUri: String?,
    private val onSelect: (OverlayEffect) -> Unit
) : RecyclerView.Adapter<EffectAdapter.VH>() {

    private val effects = OverlayEffect.entries
    private var selected = OverlayEffect.NONE

    fun setSelected(effect: OverlayEffect) {
        val old = effects.indexOf(selected)
        selected = effect
        notifyItemChanged(old)
        notifyItemChanged(effects.indexOf(effect))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_effect, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = effects.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val effect = effects[position]
        holder.label.text = effect.label
        if (previewUri != null) {
            holder.image.load(previewUri) { crossfade(120) }
        }
        holder.image.colorFilter = effect.colorFilter()
        val active = effect == selected
        holder.card.strokeWidth = if (active) 2.dp else 1.dp
        holder.card.setStrokeColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (active) R.color.brand_purple else R.color.glass_stroke
            )
        )
        holder.itemView.setOnClickListener { onSelect(effect) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.effectCard)
        val image: ImageView = view.findViewById(R.id.effectImage)
        val label: TextView = view.findViewById(R.id.effectLabel)
    }
}

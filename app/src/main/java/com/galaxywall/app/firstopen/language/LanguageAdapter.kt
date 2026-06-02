package com.galaxywall.app.firstopen.language

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ItemLanguageBinding

/**
 * Language list adapter. Mirrors the source app's LanguageAdapter: single-select, the chosen row is
 * activated and shows the selected icon. [currentSelect] tracks the selected position.
 */
class LanguageAdapter(
    private val context: Context,
    private val listener: OnLanguageClickListener
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private val items = ArrayList<Language>()
    var currentSelect = -1

    // Cache the check icons once instead of decoding them on every bind (smoother scrolling).
    private val selectedIcon by lazy { AppCompatResources.getDrawable(context, R.drawable.ic_select_lang) }
    private val unselectedIcon by lazy { AppCompatResources.getDrawable(context, R.drawable.ic_un_select_lang) }

    fun setItems(list: List<Language>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getAt(position: Int): Language = items[position]

    inner class LanguageViewHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            txtNameLanguage.text = item.title
            imgIconLanguage.setImageResource(item.flag)
            imgChooseLanguage.setImageDrawable(if (item.isChoose) selectedIcon else unselectedIcon)
            languageItem.isActivated = item.isChoose
            root.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                listener.onClickItemListener(items[pos], pos)
                val prev = currentSelect
                for (i in items.indices) items[i].isChoose = i == pos
                currentSelect = pos
                // Only refresh the two affected rows (no full rebind).
                if (prev in items.indices) notifyItemChanged(prev)
                notifyItemChanged(pos)
            }
        }
    }

    /** Programmatically select [position] (used by FO2 to pre-select the row passed from FO1). */
    fun selectItem(position: Int) {
        for (i in items.indices) {
            items[i].isChoose = i == position
        }
        notifyDataSetChanged()
        currentSelect = position
    }

    interface OnLanguageClickListener {
        fun onClickItemListener(language: Language, position: Int)
    }
}

package com.galaxywall.app.ui.language

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val list: MutableList<LanguageModel>,
    private val onItemSelected: (LanguageModel, Int) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = -1

    inner class LanguageViewHolder(
        val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageViewHolder {

        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return LanguageViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int
    ) {

        val item = list[position]

        with(holder.binding) {

            imgIconLanguage.setImageResource(item.icon)
            txtNameLanguage.text = item.name

            val isSelected = position == selectedPosition
            item.isSelected = isSelected

            if (isSelected) {

                imgChooseLanguage.setImageResource(
                    R.drawable.ic_select_lang
                )

            } else {

                imgChooseLanguage.setImageResource(
                    R.drawable.ic_un_select_lang
                )
            }

            languageItem.isActivated = isSelected

            root.setOnClickListener {

                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition

                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }

                notifyItemChanged(selectedPosition)
                onItemSelected(item, selectedPosition)
            }
        }
    }
}
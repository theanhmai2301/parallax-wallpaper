package com.galaxywall.app.ui.survey

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ItemSurveyBinding

/** Multi-select survey topics. [onSelectionChanged] reports how many are currently selected. */
class SurveyAdapter(
    private val list: List<SurveyModel>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder>() {

    inner class SurveyViewHolder(
        val binding: ItemSurveyBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurveyViewHolder {
        val binding = ItemSurveyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SurveyViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: SurveyViewHolder, position: Int) {
        val item = list[position]

        holder.binding.emojiSurvey.text = item.emoji
        holder.binding.txtNameSurvey.text = item.name
        holder.binding.imgChooseSurvey.setImageResource(
            if (item.isSelected) R.drawable.ic_select_lang else R.drawable.ic_un_select_lang
        )
        holder.binding.surveyItem.isActivated = item.isSelected

        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            // Multi-select: toggle this topic.
            list[pos].isSelected = !list[pos].isSelected
            notifyItemChanged(pos)
            onSelectionChanged(list.count { it.isSelected })
        }
    }
}

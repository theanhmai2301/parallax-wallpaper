package com.galaxywall.app.ui.survey

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.galaxywall.app.R
import com.galaxywall.app.databinding.ItemSurveyBinding

class SurveyAdapter(
    private val list: MutableList<SurveyModel>,
    private val onItemSelected: (SurveyModel) -> Unit
) : RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder>() {

    inner class SurveyViewHolder(
        val binding: ItemSurveyBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SurveyViewHolder {

        val binding = ItemSurveyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return SurveyViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(
        holder: SurveyViewHolder,
        position: Int
    ) {

        val item = list[position]

        holder.binding.imgIconSurvey.setImageResource(item.icon)
        holder.binding.txtNameSurvey.text = item.name

        if (item.isSelected) {

            holder.binding.imgChooseSurvey.setImageResource(
                R.drawable.ic_select_lang
            )

        } else {

            holder.binding.imgChooseSurvey.setImageResource(
                R.drawable.ic_un_select_lang
            )
        }

        holder.binding.surveyItem.isActivated = item.isSelected
        holder.binding.root.setOnClickListener {

            list.forEach {
                it.isSelected = false
            }

            item.isSelected = true

            notifyDataSetChanged()

            onItemSelected(item)
        }
    }
}
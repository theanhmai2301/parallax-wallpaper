package com.galaxywall.app.firstopen.survey

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.galaxywall.app.R

/**
 * Multi-select survey grid. Mirrors the source app's SurveyAdapter: tapping a cell toggles its
 * selection and also forwards the tap (the multi-step survey advances on tap). [onSelectionChanged]
 * reports the number selected; [itemClickListener] gets the tapped item + position.
 */
class SurveyAdapter(
    private val list: List<Survey>,
    private val onSelectionChanged: (selectedCount: Int) -> Unit,
    private val itemClickListener: ((survey: Survey, pos: Int) -> Unit)? = null
) : RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder>() {

    private val selectedItems = mutableListOf<Survey>()

    init {
        list.filter { it.isSelected }.forEach { selectedItems.add(it) }
    }

    class SurveyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAnimal: ImageView = itemView.findViewById(R.id.img_animal)
        val tvAnimalName: TextView = itemView.findViewById(R.id.tv_survey)
        val imgCheck: ImageView = itemView.findViewById(R.id.ivCheck)
        val llParent: View = itemView.findViewById(R.id.llParent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurveyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_survey, parent, false)
        return SurveyViewHolder(view)
    }

    override fun onBindViewHolder(holder: SurveyViewHolder, position: Int) {
        val item = list[position]
        holder.imgAnimal.setImageResource(item.imageResId)
        holder.tvAnimalName.text = item.name
        bindCheck(holder, item.isSelected)

        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(item, position)
            item.isSelected = !item.isSelected
            if (item.isSelected) selectedItems.add(item) else selectedItems.remove(item)
            bindCheck(holder, item.isSelected)
            onSelectionChanged(selectedItems.size)
            notifyItemChanged(position)
        }
    }

    /** Show the check only when selected; hide the indicator entirely otherwise (no stray dot). */
    private fun bindCheck(holder: SurveyViewHolder, selected: Boolean) {
        holder.imgCheck.isActivated = selected
        holder.imgCheck.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        holder.llParent.isActivated = selected
    }

    override fun getItemCount(): Int = list.size

    fun getSelectedItems(): List<Survey> = selectedItems
}

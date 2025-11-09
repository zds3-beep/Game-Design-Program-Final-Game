package com.yourgame.herojourney

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChoiceAdapter(
    private val onChoiceClick: (StoryNode) -> Unit
) : RecyclerView.Adapter<ChoiceAdapter.ChoiceViewHolder>() {

    private var choices = listOf<StoryNode>()

    fun submitList(newChoices: List<StoryNode>) {
        choices = newChoices
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_choice, parent, false)
        return ChoiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChoiceViewHolder, position: Int) {
        holder.bind(choices[position])
    }

    override fun getItemCount() = choices.size

    inner class ChoiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvChoiceTitle: TextView = itemView.findViewById(R.id.tvChoiceTitle)
        private val tvChoiceDescription: TextView = itemView.findViewById(R.id.tvChoiceDescription)

        fun bind(node: StoryNode) {
            tvCategory.text = node.category.uppercase()
            tvChoiceTitle.text = node.title
            tvChoiceDescription.text = node.description

            val categoryColor = when (node.category.lowercase()) {
                "combat" -> Color.parseColor("#D32F2F")
                "social" -> Color.parseColor("#1976D2")
                "magic" -> Color.parseColor("#7B1FA2")
                "financial" -> Color.parseColor("#388E3C")
                "nobility" -> Color.parseColor("#F57C00")
                "tragedy" -> Color.parseColor("#455A64")
                else -> Color.parseColor("#616161")
            }
            tvCategory.setBackgroundColor(categoryColor)

            itemView.setOnClickListener {
                onChoiceClick(node)
            }
        }
    }
}

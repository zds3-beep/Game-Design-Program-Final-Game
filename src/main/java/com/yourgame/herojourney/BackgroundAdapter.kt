package com.yourgame.herojourney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BackgroundAdapter(
    private val onBackgroundClick: (HeroBackground) -> Unit
) : RecyclerView.Adapter<BackgroundAdapter.BackgroundViewHolder>() {

    private var backgrounds = listOf<HeroBackground>()

    fun submitList(newBackgrounds: List<HeroBackground>) {
        backgrounds = newBackgrounds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackgroundViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_background, parent, false)
        return BackgroundViewHolder(view)
    }

    override fun onBindViewHolder(holder: BackgroundViewHolder, position: Int) {
        holder.bind(backgrounds[position])
    }

    override fun getItemCount() = backgrounds.size

    inner class BackgroundViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBackgroundName: TextView = itemView.findViewById(R.id.tvBackgroundName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvFlavorText: TextView = itemView.findViewById(R.id.tvFlavorText)
        private val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
        private val tvStatsPreview: TextView = itemView.findViewById(R.id.tvStatsPreview)

        fun bind(background: HeroBackground) {
            tvBackgroundName.text = background.name
            tvDescription.text = background.description
            tvFlavorText.text = "\"${background.flavorText}\""

            // Difficulty badge
            tvDifficulty.text = background.difficulty.displayName
            tvDifficulty.setBackgroundColor(background.difficulty.color)

            // Stats preview
            val stats = background.initialStats
            tvStatsPreview.text = buildString {
                appendLine("Starting Stats:")
                appendLine("⚔️ Martial: ${stats.martial}")
                appendLine("🛡️ Equipment: ${stats.equipment}")
                appendLine("💬 Social: ${stats.social}")
                appendLine("💰 Financial: ${stats.financial}")
                appendLine("👑 Nobility: ${stats.nobility}")
                appendLine("✨ Magical: ${stats.magical}")
                appendLine()
                appendLine("💪 Motivation: ${stats.motivation}")
                appendLine("😵 Madness: ${stats.madness}")
                appendLine("🧠 Insight: ${stats.insight}")
            }

            itemView.setOnClickListener {
                onBackgroundClick(background)
            }
        }
    }
}
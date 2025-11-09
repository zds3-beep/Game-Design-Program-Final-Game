package com.yourgame.herojourney

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SaveFileAdapter(
    private val onLoadClick: (SaveFileMetadata) -> Unit,
    private val onDeleteClick: (SaveFileMetadata) -> Unit
) : RecyclerView.Adapter<SaveFileAdapter.SaveFileViewHolder>() {

    private var saveFiles = listOf<SaveFileMetadata>()

    fun submitList(files: List<SaveFileMetadata>) {
        saveFiles = files
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaveFileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_save_file, parent, false)
        return SaveFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaveFileViewHolder, position: Int) {
        holder.bind(saveFiles[position])
    }

    override fun getItemCount() = saveFiles.size

    inner class SaveFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSaveName: TextView = itemView.findViewById(R.id.tvSaveName)
        private val tvSaveDate: TextView = itemView.findViewById(R.id.tvSaveDate)
        private val tvSaveInfo: TextView = itemView.findViewById(R.id.tvSaveInfo)
        private val btnLoad: Button = itemView.findViewById(R.id.btnLoad)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(metadata: SaveFileMetadata) {
            tvSaveName.text = metadata.saveName
            tvSaveDate.text = formatDate(metadata.saveDate)
            tvSaveInfo.text = "Month ${metadata.currentMonth} • Hero Level ${metadata.heroLevel}"

            btnLoad.setOnClickListener {
                onLoadClick(metadata)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(metadata)
            }
        }

        private fun formatDate(timestamp: Long): String {
            val format = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
            return format.format(Date(timestamp))
        }
    }
}

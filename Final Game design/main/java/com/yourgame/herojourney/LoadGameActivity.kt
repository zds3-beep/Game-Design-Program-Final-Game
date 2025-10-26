package com.yourgame.herojourney

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LoadGameActivity : AppCompatActivity() {

    private lateinit var saveManager: SaveManager
    private lateinit var adapter: SaveFileAdapter
    private lateinit var rvSaveFiles: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_game)

        saveManager = SaveManager(this)

        rvSaveFiles = findViewById(R.id.rvSaveFiles)
        setupRecyclerView()

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        refreshSaveList()
    }

    private fun setupRecyclerView() {
        adapter = SaveFileAdapter(
            onLoadClick = { metadata -> loadSaveFile(metadata) },
            onDeleteClick = { metadata -> confirmDeleteSave(metadata) }
        )
        rvSaveFiles.adapter = adapter
        rvSaveFiles.layoutManager = LinearLayoutManager(this)
    }

    private fun refreshSaveList() {
        val saves = saveManager.getAllSaves()
        adapter.submitList(saves)

        if (saves.isEmpty()) {
            Toast.makeText(this, "No save files found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSaveFile(metadata: SaveFileMetadata) {
        val slotNumber = metadata.fileName.replace("save_slot_", "")
            .replace(".json", "")
            .toIntOrNull() ?: return

        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LOAD_GAME", true)
        intent.putExtra("SAVE_SLOT", slotNumber)
        startActivity(intent)
        finish()
    }

    private fun confirmDeleteSave(metadata: SaveFileMetadata) {
        AlertDialog.Builder(this)
            .setTitle("Delete Save")
            .setMessage("Are you sure you want to delete '${metadata.saveName}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSaveFile(metadata)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSaveFile(metadata: SaveFileMetadata) {
        val slotNumber = metadata.fileName.replace("save_slot_", "")
            .replace(".json", "")
            .toIntOrNull() ?: return

        val success = saveManager.deleteSave(slotNumber)

        if (success) {
            Toast.makeText(this, "Save deleted", Toast.LENGTH_SHORT).show()
            refreshSaveList()
        } else {
            Toast.makeText(this, "Failed to delete save", Toast.LENGTH_SHORT).show()
        }
    }
}

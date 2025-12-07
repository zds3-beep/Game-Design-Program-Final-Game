package com.yourgame.herojourney

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LoadGameActivity : AppCompatActivity() {

    private lateinit var saveManager: SaveManager
    private lateinit var saveFileAdapter: SaveFileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_game)

        saveManager = SaveManager(this)

        setupRecyclerView()
        loadSaves()
    }

    private fun setupRecyclerView() {
        val rvSaves = findViewById<RecyclerView>(R.id.rvSaveFiles)
        saveFileAdapter = SaveFileAdapter(
            onLoadClick = { saveFile -> loadSelectedGame(saveFile.fileName) },
            onDeleteClick = { saveFile -> confirmDelete(saveFile.fileName) }
        )
        rvSaves.adapter = saveFileAdapter
        rvSaves.layoutManager = LinearLayoutManager(this)
    }

    private fun loadSaves() {
        val saves = saveManager.getAllSaves()
        saveFileAdapter.submitList(saves)
    }

    private fun loadSelectedGame(fileName: String) {
        val slot = fileName.filter { it.isDigit() }.toIntOrNull() ?: return
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LOAD_GAME", true)
        intent.putExtra("SAVE_SLOT", slot)
        startActivity(intent)
        finish()
    }

    private fun confirmDelete(fileName: String) {
        val slot = fileName.filter { it.isDigit() }.toIntOrNull() ?: return
        // You can add a confirmation dialog here if you want
        saveManager.deleteSave(slot)
        loadSaves()
    }
}
package com.yourgame.herojourney

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BackgroundSelectionActivity : AppCompatActivity() {

    private lateinit var rvBackgrounds: RecyclerView
    private lateinit var adapter: BackgroundAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_selection)

        setupRecyclerView()
        loadBackgrounds()
    }

    private fun setupRecyclerView() {
        rvBackgrounds = findViewById(R.id.rvBackgrounds)
        adapter = BackgroundAdapter { background ->
            onBackgroundSelected(background)
        }
        rvBackgrounds.adapter = adapter
        rvBackgrounds.layoutManager = LinearLayoutManager(this)
    }

    private fun loadBackgrounds() {
        val backgrounds = BackgroundRepository.getAllBackgrounds()
        adapter.submitList(backgrounds)
    }

    private fun onBackgroundSelected(background: HeroBackground) {
        // Start game with selected background
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("NEW_GAME", true)
        intent.putExtra("BACKGROUND_ID", background.id)
        startActivity(intent)
        finish()
    }
}
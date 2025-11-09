package com.yourgame.herojourney

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // New Game now goes to background selection
        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            startBackgroundSelection()
        }

        // ... rest of menu buttons
    }

    private fun startBackgroundSelection() {
        val intent = Intent(this, BackgroundSelectionActivity::class.java)
        startActivity(intent)
    }
}
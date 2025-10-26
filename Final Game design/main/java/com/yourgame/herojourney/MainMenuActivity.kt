package com.yourgame.herojourney

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.view.*
import android.graphics.Color
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainMenuActivity : AppCompatActivity() {

    private lateinit var saveManager: SaveManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        saveManager = SaveManager(this)

        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            startNewGame()
        }

        findViewById<Button>(R.id.btnLoadGame).setOnClickListener {
            openLoadGameScreen()
        }

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            openSettings()
        }

        findViewById<Button>(R.id.btnExit).setOnClickListener {
            finish()
        }
    }

    private fun startNewGame() {
        if (saveManager.hasSave(0)) {
            AlertDialog.Builder(this)
                .setTitle("New Game")
                .setMessage("Starting a new game will overwrite your autosave. Continue?")
                .setPositiveButton("Yes") { _, _ ->
                    launchNewGame()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            launchNewGame()
        }
    }

    private fun launchNewGame() {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("NEW_GAME", true)
        startActivity(intent)
    }

    private fun openLoadGameScreen() {
        val intent = Intent(this, LoadGameActivity::class.java)
        startActivity(intent)
    }

    private fun openSettings() {
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
    }
}
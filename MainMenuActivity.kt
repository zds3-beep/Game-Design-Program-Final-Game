package com.yourgame.herojourney

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Ensure music is initialized and playing
        AudioManager.initialize(this)
        AudioManager.playBackgroundMusic()

        // New Game now goes to the intro video
        findViewById<Button>(R.id.btnNewGame).setOnClickListener {
            startIntroVideo()
        }

        findViewById<Button>(R.id.btnLoadGame).setOnClickListener {
            val intent = Intent(this, LoadGameActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnExit).setOnClickListener {
            finishAffinity() // Closes the app
        }
    }

    override fun onPause() {
        super.onPause()
        AudioManager.pauseBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        AudioManager.playBackgroundMusic()
    }

    private fun startIntroVideo() {
        val intent = Intent(this, IntroVideoActivity::class.java)
        startActivity(intent)
    }
}

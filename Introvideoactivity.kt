package com.yourgame.herojourney

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class IntroVideoActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var tvSkipHint: TextView
    private var videoCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro_video)

        // Hide system UI for immersive experience
        hideSystemUI()

        videoView = findViewById(R.id.videoView)
        tvSkipHint = findViewById(R.id.tvSkipHint)

        setupVideo()
        setupTapToSkip()
        fadeOutSkipHint()
    }

    private fun fadeOutSkipHint() {
        // Fade out the "tap to skip" hint after 3 seconds
        tvSkipHint.postDelayed({
            tvSkipHint.animate()
                .alpha(0f)
                .setDuration(1000)
                .start()
        }, 3000)
    }

    private fun setupVideo() {
        // Set the video path from res/raw folder
        val videoUri = Uri.parse("android.resource://$packageName/${R.raw.intro_video}")
        videoView.setVideoURI(videoUri)

        // Start playing immediately
        videoView.start()

        // Stop background music during video
        AudioManager.pauseBackgroundMusic()

        // Listen for video completion
        videoView.setOnCompletionListener {
            videoCompleted = true
            proceedToBackgroundSelection()
        }

        // Handle errors gracefully
        videoView.setOnErrorListener { mp, what, extra ->
            // If video fails to load, skip to background selection
            proceedToBackgroundSelection()
            true // Return true to indicate error was handled
        }
    }

    private fun setupTapToSkip() {
        // Make the entire screen tappable to skip
        val rootView = findViewById<View>(android.R.id.content)

        rootView.setOnClickListener {
            if (!videoCompleted) {
                skipVideo()
            }
        }

        // Also make videoView tappable
        videoView.setOnClickListener {
            if (!videoCompleted) {
                skipVideo()
            }
        }
    }

    private fun skipVideo() {
        // Stop the video
        if (videoView.isPlaying) {
            videoView.stopPlayback()
        }

        proceedToBackgroundSelection()
    }

    private fun proceedToBackgroundSelection() {
        // Resume background music
        AudioManager.playBackgroundMusic()

        // Navigate to background selection
        val intent = Intent(this, BackgroundSelectionActivity::class.java)
        startActivity(intent)
        finish() // Don't allow back button to return to video
    }

    private fun hideSystemUI() {
        // Make the video fullscreen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    override fun onBackPressed() {
        // Disable back button during video
        // Player must watch or tap to skip
    }

    override fun onPause() {
        super.onPause()
        // Pause video if app goes to background
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume video if returning to app
        if (!videoCompleted) {
            videoView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up video resources
        videoView.stopPlayback()
    }
}
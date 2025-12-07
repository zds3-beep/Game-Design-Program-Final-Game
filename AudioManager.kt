package com.yourgame.herojourney

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

object AudioManager {
    private var backgroundMusic: MediaPlayer? = null
    private var successSound: MediaPlayer? = null
    private var failSound: MediaPlayer? = null
    private var bossMusic: MediaPlayer? = null

    private var isInitialized = false
    private var isMusicEnabled = true
    private var areSoundsEnabled = true

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        try {
            backgroundMusic = MediaPlayer.create(context, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
            }

            successSound = MediaPlayer.create(context, R.raw.success_chime)?.apply {
                setVolume(0.7f, 0.7f)
            }

            failSound = MediaPlayer.create(context, R.raw.fail_sound)?.apply {
                setVolume(0.7f, 0.7f)
            }

            bossMusic = MediaPlayer.create(context, R.raw.boss_music)?.apply {
                isLooping = true
                setVolume(0.3f, 0.3f)
            }

            if (backgroundMusic == null) {
                Log.w("AudioManager", "Background music file not found - audio disabled")
            }
            if (successSound == null) {
                Log.w("AudioManager", "Success sound file not found - audio disabled")
            }
            if (failSound == null) {
                Log.w("AudioManager", "Fail sound file not found - audio disabled")
            }
            if (bossMusic == null) {
                Log.w("AudioManager", "Boss music file not found - audio disabled")
            }

            Log.d("AudioManager", "Audio initialized successfully")
        } catch (e: Exception) {
            Log.e("AudioManager", "Error initializing audio: ${e.message}")
        }
    }

    fun playBackgroundMusic() {
        if (isMusicEnabled && backgroundMusic?.isPlaying == false) {
            try {
                pauseBossMusic()
                backgroundMusic?.start()
                Log.d("AudioManager", "Background music started")
            } catch (e: Exception) {
                Log.e("AudioManager", "Error starting music: ${e.message}")
            }
        }
    }

    fun pauseBackgroundMusic() {
        if (backgroundMusic?.isPlaying == true) {
            backgroundMusic?.pause()
            Log.d("AudioManager", "Background music paused")
        }
    }

    fun playBossMusic() {
        if (isMusicEnabled && bossMusic?.isPlaying == false) {
            try {
                pauseBackgroundMusic()
                bossMusic?.start()
                Log.d("AudioManager", "Boss music started")
            } catch (e: Exception) {
                Log.e("AudioManager", "Error starting boss music: ${e.message}")
            }
        }
    }

    fun pauseBossMusic() {
        if (bossMusic?.isPlaying == true) {
            bossMusic?.pause()
            Log.d("AudioManager", "Boss music paused")
        }
    }

    fun playSuccessSound() {
        if (areSoundsEnabled) {
            try {
                successSound?.let {
                    if (it.isPlaying) {
                        it.seekTo(0)
                    }
                    it.start()
                }
                Log.d("AudioManager", "Success sound played")
            } catch (e: Exception) {
                Log.e("AudioManager", "Error playing success sound: ${e.message}")
            }
        }
    }

    fun playFailSound() {
        if (areSoundsEnabled) {
            try {
                failSound?.let {
                    if (it.isPlaying) {
                        it.seekTo(0)
                    }
                    it.start()
                }
                Log.d("AudioManager", "Fail sound played")
            } catch (e: Exception) {
                Log.e("AudioManager", "Error playing fail sound: ${e.message}")
            }
        }
    }

    fun toggleMusic(): Boolean {
        isMusicEnabled = !isMusicEnabled
        if (!isMusicEnabled) {
            pauseBackgroundMusic()
            pauseBossMusic()
        } else {
            playBackgroundMusic()
        }
        return isMusicEnabled
    }

    fun toggleSounds(): Boolean {
        areSoundsEnabled = !areSoundsEnabled
        return areSoundsEnabled
    }

    fun setMusicVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        backgroundMusic?.setVolume(clampedVolume, clampedVolume)
        bossMusic?.setVolume(clampedVolume, clampedVolume)
    }

    fun setSoundVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        successSound?.setVolume(clampedVolume, clampedVolume)
        failSound?.setVolume(clampedVolume, clampedVolume)
    }

    fun release() {
        backgroundMusic?.release()
        successSound?.release()
        failSound?.release()
        bossMusic?.release()

        backgroundMusic = null
        successSound = null
        failSound = null
        bossMusic = null

        isInitialized = false
        Log.d("AudioManager", "Audio resources released")
    }
}

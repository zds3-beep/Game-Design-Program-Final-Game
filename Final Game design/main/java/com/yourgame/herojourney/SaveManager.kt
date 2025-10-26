package com.yourgame.herojourney

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class GameSaveData(
    val saveDate: Long = System.currentTimeMillis(),
    val saveName: String = "",
    val heroStats: HeroStats,
    val currentMonth: Int,
    val maxMonths: Int,
    val completedNodeIds: List<String>,
    val availableNodeIds: List<String>,
    val lockedNodeIds: List<String>,
    val choiceHistory: List<ChoiceRecord>,
    val totalPlayTime: Long = 0L
)

data class SaveFileMetadata(
    val fileName: String,
    val saveName: String,
    val saveDate: Long,
    val currentMonth: Int,
    val heroLevel: Int
)

sealed class SaveResult {
    data class Success(val message: String, val saveFile: File) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

sealed class LoadResult {
    data class Success(val message: String, val saveData: GameSaveData) : LoadResult()
    data class Error(val message: String) : LoadResult()
}

class SaveManager(private val context: Context) {

    private val gson = Gson()
    private val saveDirectory = File(context.filesDir, "saves")
    private val prefs: SharedPreferences =
        context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_LAST_SAVE_SLOT = "last_save_slot"
        private const val MAX_SAVE_SLOTS = 10
    }

    init {
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs()
        }
    }

    fun saveGame(
        gameManager: GameManager,
        slotNumber: Int = 0,
        saveName: String? = null
    ): SaveResult {
        try {
            if (slotNumber < 0 || slotNumber > MAX_SAVE_SLOTS) {
                return SaveResult.Error("Invalid save slot: $slotNumber")
            }

            val saveData = GameSaveData(
                saveDate = System.currentTimeMillis(),
                saveName = saveName ?: generateDefaultSaveName(),
                heroStats = gameManager.heroStats.copy(),
                currentMonth = gameManager.currentMonth,
                maxMonths = gameManager.maxMonths,
                completedNodeIds = gameManager.completedNodes.toList(),
                availableNodeIds = gameManager.availableNodes.toList(),
                lockedNodeIds = gameManager.lockedNodes.toList(),
                choiceHistory = gameManager.choiceHistory.toList(),
                totalPlayTime = prefs.getLong("play_time", 0L)
            )

            val json = gson.toJson(saveData)

            val fileName = "save_slot_$slotNumber.json"
            val saveFile = File(saveDirectory, fileName)
            saveFile.writeText(json)

            prefs.edit().putInt(PREF_LAST_SAVE_SLOT, slotNumber).apply()

            return SaveResult.Success(
                message = "Game saved to slot $slotNumber",
                saveFile = saveFile
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return SaveResult.Error("Failed to save game: ${e.message}")
        }
    }

    fun autoSave(gameManager: GameManager): SaveResult {
        return saveGame(gameManager, slotNumber = 0, saveName = "AutoSave")
    }

    private fun generateDefaultSaveName(): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return "Save ${dateFormat.format(Date())}"
    }

    fun loadGame(gameManager: GameManager, slotNumber: Int): LoadResult {
        try {
            val fileName = "save_slot_$slotNumber.json"
            val saveFile = File(saveDirectory, fileName)

            if (!saveFile.exists()) {
                return LoadResult.Error("No save found in slot $slotNumber")
            }

            val json = saveFile.readText()
            val saveData = gson.fromJson(json, GameSaveData::class.java)

            restoreGameState(gameManager, saveData)

            return LoadResult.Success(
                message = "Game loaded from slot $slotNumber",
                saveData = saveData
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return LoadResult.Error("Failed to load game: ${e.message}")
        }
    }

    private fun restoreGameState(gameManager: GameManager, saveData: GameSaveData) {
        gameManager.heroStats.martial = saveData.heroStats.martial
        gameManager.heroStats.equipment = saveData.heroStats.equipment
        gameManager.heroStats.social = saveData.heroStats.social
        gameManager.heroStats.financial = saveData.heroStats.financial
        gameManager.heroStats.nobility = saveData.heroStats.nobility
        gameManager.heroStats.magical = saveData.heroStats.magical
        gameManager.heroStats.motivation = saveData.heroStats.motivation
        gameManager.heroStats.madness = saveData.heroStats.madness
        gameManager.heroStats.insight = saveData.heroStats.insight

        gameManager.currentMonth = saveData.currentMonth

        gameManager.completedNodes.clear()
        gameManager.completedNodes.addAll(saveData.completedNodeIds)

        gameManager.availableNodes.clear()
        gameManager.availableNodes.addAll(saveData.availableNodeIds)

        gameManager.lockedNodes.clear()
        gameManager.lockedNodes.addAll(saveData.lockedNodeIds)

        gameManager.choiceHistory.clear()
        gameManager.choiceHistory.addAll(saveData.choiceHistory)

        prefs.edit().putLong("play_time", saveData.totalPlayTime).apply()
    }

    fun getAllSaves(): List<SaveFileMetadata> {
        val saves = mutableListOf<SaveFileMetadata>()

        for (i in 0..MAX_SAVE_SLOTS) {
            val metadata = getSaveMetadata(i)
            if (metadata != null) {
                saves.add(metadata)
            }
        }

        return saves.sortedByDescending { it.saveDate }
    }

    fun getSaveMetadata(slotNumber: Int): SaveFileMetadata? {
        try {
            val fileName = "save_slot_$slotNumber.json"
            val saveFile = File(saveDirectory, fileName)

            if (!saveFile.exists()) return null

            val json = saveFile.readText()
            val saveData = gson.fromJson(json, GameSaveData::class.java)

            val heroLevel = with(saveData.heroStats) {
                (martial + equipment + social + financial + nobility + magical) / 6
            }

            return SaveFileMetadata(
                fileName = fileName,
                saveName = saveData.saveName,
                saveDate = saveData.saveDate,
                currentMonth = saveData.currentMonth,
                heroLevel = heroLevel
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun hasSave(slotNumber: Int): Boolean {
        val fileName = "save_slot_$slotNumber.json"
        return File(saveDirectory, fileName).exists()
    }

    fun deleteSave(slotNumber: Int): Boolean {
        try {
            val fileName = "save_slot_$slotNumber.json"
            val saveFile = File(saveDirectory, fileName)
            return saveFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
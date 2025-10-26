package com.yourgame.herojourney

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class GameActivity : AppCompatActivity() {

    private lateinit var gameManager: GameManager
    private lateinit var saveManager: SaveManager
    private lateinit var choiceAdapter: ChoiceAdapter

    private lateinit var btnSaveClose: Button

    private lateinit var tvMonthCounter: TextView
    private lateinit var tvMartial: TextView
    private lateinit var tvEquipment: TextView
    private lateinit var tvSocial: TextView
    private lateinit var tvFinancial: TextView
    private lateinit var tvNobility: TextView
    private lateinit var tvMagical: TextView
    private lateinit var progressMotivation: ProgressBar
    private lateinit var progressMadness: ProgressBar
    private lateinit var progressInsight: ProgressBar
    private lateinit var rvChoices: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        try {
            initializeViews()

            gameManager = GameManager(this)
            saveManager = SaveManager(this)
            btnSaveClose.setOnClickListener {
                saveAndClose()
            }

            setupRecyclerView()

            val isNewGame = intent.getBooleanExtra("NEW_GAME", false)
            val isLoadGame = intent.getBooleanExtra("LOAD_GAME", false)
            val saveSlot = intent.getIntExtra("SAVE_SLOT", 0)

            when {
                isLoadGame -> loadGame(saveSlot)
                isNewGame -> startNewRound()
                else -> loadAutoSave()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showFatalErrorDialog(e)
        }
    }

    private fun initializeViews() {
        tvMonthCounter = findViewById(R.id.tvMonthCounter)
        tvMartial = findViewById(R.id.tvMartial)
        tvEquipment = findViewById(R.id.tvEquipment)
        tvSocial = findViewById(R.id.tvSocial)
        btnSaveClose = findViewById(R.id.btnSaveClose)
        tvFinancial = findViewById(R.id.tvFinancial)
        tvNobility = findViewById(R.id.tvNobility)
        tvMagical = findViewById(R.id.tvMagical)
        progressMotivation = findViewById(R.id.progressMotivation)
        progressMadness = findViewById(R.id.progressMadness)
        progressInsight = findViewById(R.id.progressInsight)
        rvChoices = findViewById(R.id.rvChoices)
    }

    private fun setupRecyclerView() {
        choiceAdapter = ChoiceAdapter { node ->
            onChoiceSelected(node)
        }
        rvChoices.adapter = choiceAdapter
        rvChoices.layoutManager = LinearLayoutManager(this)
    }

    private fun startNewRound() {
        when (val result = gameManager.startNewRound()) {
            is GameRoundResult.Continue -> {
                if (result.availableChoices.isEmpty()) {
                    showGameOverDialog(
                        "You've run out of opportunities.",
                        gameManager.generateStoryLog()
                    )
                } else {
                    updateUI(result.month, result.maxMonths, result.heroStats)
                    choiceAdapter.submitList(result.availableChoices)
                }
            }
            is GameRoundResult.GameOver -> {
                showGameOverDialog(result.message, result.storyLog)
            }
        }
    }

    private fun onChoiceSelected(node: StoryNode) {
        val oldStats = gameManager.heroStats.copy()

        when (val result = gameManager.makeChoice(node.id)) {
            is ChoiceResult.Success -> {
                showResultDialog(
                    node = node,
                    outcome = result.outcome,
                    passedCheck = result.passedCheck,
                    oldStats = oldStats,
                    newStats = result.updatedStats
                )
            }
            is ChoiceResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(month: Int, maxMonths: Int, stats: HeroStats) {
        tvMonthCounter.text = "Month: $month / $maxMonths"

        tvMartial.text = stats.martial.toString()
        tvEquipment.text = stats.equipment.toString()
        tvSocial.text = stats.social.toString()
        tvFinancial.text = stats.financial.toString()
        tvNobility.text = stats.nobility.toString()
        tvMagical.text = stats.magical.toString()

        progressMotivation.progress = stats.motivation
        progressMadness.progress = stats.madness
        progressInsight.progress = stats.insight
    }

    private fun showResultDialog(
        node: StoryNode,
        outcome: Outcome,
        passedCheck: StatType?,
        oldStats: HeroStats,
        newStats: HeroStats
    ) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_choice_result)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvResultHeader = dialog.findViewById<TextView>(R.id.tvResultHeader)
        tvResultHeader.text = if (passedCheck != null) "SUCCESS!" else "OUTCOME"
        tvResultHeader.setTextColor(
            if (passedCheck != null) Color.parseColor("#4CAF50")
            else Color.parseColor("#FF9800")
        )

        dialog.findViewById<TextView>(R.id.tvOutcomeDescription).text =
            "You chose to: ${outcome.description}"

        val resultTextView = dialog.findViewById<TextView>(R.id.tvResultText)
        resultTextView.text = outcome.effects?.resultText ?: "(No result text provided)"

        val layoutPassedCheck = dialog.findViewById<LinearLayout>(R.id.layoutPassedCheck)
        val tvPassedCheck = dialog.findViewById<TextView>(R.id.tvPassedCheck)

        if (layoutPassedCheck != null && tvPassedCheck != null) {
            if (passedCheck != null) {
                layoutPassedCheck.visibility = View.VISIBLE
                val bonusText = outcome.difficulty?.statBonus?.let { " (+$it bonus)" } ?: ""
                tvPassedCheck.text = "${passedCheck.name} Check Passed!$bonusText"
            } else {
                layoutPassedCheck.visibility = View.GONE
            }
        }

        val layoutStatChanges = dialog.findViewById<LinearLayout>(R.id.layoutStatChanges)
        layoutStatChanges.removeAllViews()

        addStatChangeRow(layoutStatChanges, "Martial", oldStats.martial, newStats.martial)
        addStatChangeRow(layoutStatChanges, "Equipment", oldStats.equipment, newStats.equipment)
        addStatChangeRow(layoutStatChanges, "Social", oldStats.social, newStats.social)
        addStatChangeRow(layoutStatChanges, "Financial", oldStats.financial, newStats.financial)
        addStatChangeRow(layoutStatChanges, "Nobility", oldStats.nobility, newStats.nobility)
        addStatChangeRow(layoutStatChanges, "Magical", oldStats.magical, newStats.magical)

        val layoutBarChanges = dialog.findViewById<LinearLayout>(R.id.layoutBarChanges)
        if (layoutBarChanges != null) {
            layoutBarChanges.removeAllViews()
            addStatChangeRow(layoutBarChanges, "Motivation", oldStats.motivation, newStats.motivation)
            addStatChangeRow(layoutBarChanges, "Madness", oldStats.madness, newStats.madness)
            addStatChangeRow(layoutBarChanges, "Insight", oldStats.insight, newStats.insight)
        }

        dialog.findViewById<Button>(R.id.btnContinue).setOnClickListener {
            dialog.dismiss()
            startNewRound()
        }

        dialog.show()
    }

    private fun addStatChangeRow(
        parent: LinearLayout,
        statName: String,
        oldValue: Int,
        newValue: Int
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 8, 0, 8)
        }

        val tvName = TextView(this).apply {
            text = "$statName:"
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val change = newValue - oldValue
        val tvChange = TextView(this).apply {
            text = if (change > 0) "+$change" else change.toString()
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(
                when {
                    change > 0 -> Color.parseColor("#4CAF50")
                    change < 0 -> Color.parseColor("#F44336")
                    else -> Color.GRAY
                }
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvRange = TextView(this).apply {
            text = "  ($oldValue → $newValue)"
            textSize = 12f
            setTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
            }
        }

        row.addView(tvName)
        row.addView(tvChange)
        row.addView(tvRange)
        parent.addView(row)
    }

    private fun showGameOverDialog(message: String, storyLog: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Game Over")
        builder.setMessage(message)
        builder.setPositiveButton("View Story") { _, _ ->
            showStoryLogDialog(storyLog)
        }
        builder.setNegativeButton("Main Menu") { _, _ ->
            returnToMainMenu()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun showStoryLogDialog(storyLog: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Your Hero's Journey")

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = storyLog
            setPadding(40, 40, 40, 40)
            textSize = 14f
        }
        scrollView.addView(textView)

        builder.setView(scrollView)
        builder.setPositiveButton("Main Menu") { _, _ ->
            returnToMainMenu()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun loadGame(slotNumber: Int) {
        when (val result = saveManager.loadGame(gameManager, slotNumber)) {
            is LoadResult.Success -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                updateUI(
                    gameManager.currentMonth,
                    gameManager.maxMonths,
                    gameManager.heroStats
                )
                startNewRound()
            }
            is LoadResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                startNewRound()
            }
        }
    }

    private fun loadAutoSave() {
        if (saveManager.hasSave(0)) {
            loadGame(0)
        } else {
            startNewRound()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.game_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                showSaveDialog()
                true
            }
            R.id.action_quit -> {
                confirmQuit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSaveDialog() {
        val slots = arrayOf(
            "Slot 1", "Slot 2", "Slot 3", "Slot 4", "Slot 5",
            "Slot 6", "Slot 7", "Slot 8", "Slot 9", "Slot 10"
        )

        AlertDialog.Builder(this)
            .setTitle("Save Game")
            .setItems(slots) { _, which ->
                saveToSlot(which + 1)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToSlot(slotNumber: Int) {
        when (val result = saveManager.saveGame(gameManager, slotNumber)) {
            is SaveResult.Success -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
            is SaveResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveAndClose() {
        // Save the game
        when (val result = saveManager.autoSave(gameManager)) {
            is SaveResult.Success -> {
                Toast.makeText(this, "Game saved!", Toast.LENGTH_SHORT).show()
                // Small delay so user sees the toast
                Handler(Looper.getMainLooper()).postDelayed({
                    returnToMainMenu()
                }, 500)
            }
            is SaveResult.Error -> {
                Toast.makeText(this, "Save failed: ${result.message}", Toast.LENGTH_LONG).show()
                // Still allow them to quit
                returnToMainMenu()
            }
        }
    }

    private fun confirmQuit() {
        AlertDialog.Builder(this)
            .setTitle("Quit Game")
            .setMessage("Do you want to save before quitting?")
            .setPositiveButton("Save & Quit") { _, _ ->
                saveManager.autoSave(gameManager)
                returnToMainMenu()
            }
            .setNegativeButton("Quit Without Saving") { _, _ ->
                returnToMainMenu()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun returnToMainMenu() {
        val intent = Intent(this, MainMenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun showFatalErrorDialog(e: Exception) {
        AlertDialog.Builder(this)
            .setTitle("Fatal Error")
            .setMessage("An unexpected error occurred and the game cannot continue.\n\nDetails: ${e.message}")
            .setPositiveButton("Return to Menu") { _, _ ->
                returnToMainMenu()
            }
            .setCancelable(false)
            .show()
    }
}

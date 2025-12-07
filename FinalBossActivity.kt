package com.yourgame.herojourney

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FinalBossActivity : AppCompatActivity() {

    private lateinit var heroStats: HeroStats

    // Views
    private lateinit var tvBossTitle: TextView
    private lateinit var tvBossDescription: TextView
    private lateinit var imgBoss: ImageView
    private lateinit var scrollOptions: ScrollView
    private lateinit var btnHandToHand: Button
    private lateinit var btnArmy: Button
    private lateinit var btnMercenaries: Button
    private lateinit var btnMagicSeal: Button
    private lateinit var btnFirearms: Button
    private lateinit var btnBefriend: Button
    private lateinit var btnMartialMagic: Button
    private lateinit var btnArmyMercenaries: Button
    private lateinit var btnCeremony: Button
    private lateinit var btnMagicalWeapons: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_boss)

        // Retrieve stats from Intent
        heroStats = HeroStats(
            _martial = intent.getIntExtra("MARTIAL", 10),
            _equipment = intent.getIntExtra("EQUIPMENT", 10),
            _social = intent.getIntExtra("SOCIAL", 10),
            _financial = intent.getIntExtra("FINANCIAL", 10),
            _nobility = intent.getIntExtra("NOBILITY", 10),
            _magical = intent.getIntExtra("MAGICAL", 10),
            _motivation = intent.getIntExtra("MOTIVATION", 50),
            _madness = intent.getIntExtra("MADNESS", 0),
            _insight = intent.getIntExtra("INSIGHT", 0)
        )

        initializeViews()
        setupButtons()

        // Start boss music as soon as the activity loads
        AudioManager.playBossMusic()
    }

    private fun initializeViews() {
        tvBossTitle = findViewById(R.id.tvBossTitle)
        tvBossDescription = findViewById(R.id.tvBossDescription)
        imgBoss = findViewById(R.id.imgBoss)
        scrollOptions = findViewById(R.id.scrollOptions)

        btnHandToHand = findViewById(R.id.btnHandToHand)
        btnArmy = findViewById(R.id.btnArmy)
        btnMercenaries = findViewById(R.id.btnMercenaries)
        btnMagicSeal = findViewById(R.id.btnMagicSeal)
        btnFirearms = findViewById(R.id.btnFirearms)
        btnBefriend = findViewById(R.id.btnBefriend)
        btnMartialMagic = findViewById(R.id.btnMartialMagic)
        btnArmyMercenaries = findViewById(R.id.btnArmyMercenaries)
        btnCeremony = findViewById(R.id.btnCeremony)
        btnMagicalWeapons = findViewById(R.id.btnMagicalWeapons)
    }

    private fun setupButtons() {
        // Single stat checks (higher difficulty)
        btnHandToHand.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.HAND_TO_HAND)
        }
        btnArmy.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.ARMY)
        }
        btnMercenaries.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.MERCENARIES)
        }
        btnMagicSeal.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.MAGIC_SEAL)
        }
        btnFirearms.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.FIREARMS)
        }
        btnBefriend.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.BEFRIEND)
        }

        // Dual stat checks (lower difficulty)
        btnMartialMagic.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.MARTIAL_MAGIC)
        }
        btnArmyMercenaries.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.ARMY_MERCENARIES)
        }
        btnCeremony.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.CEREMONY)
        }
        btnMagicalWeapons.setOnClickListener {
            attemptFinalBoss(FinalBossStrategy.MAGICAL_WEAPONS)
        }
    }

    private fun attemptFinalBoss(strategy: FinalBossStrategy) {
        val result = evaluateStrategy(strategy, heroStats)

        if (result.success) {
            AudioManager.playSuccessSound()
            showVictoryDialog(strategy, result)
        } else {
            AudioManager.playFailSound()
            showDefeatDialog(strategy, result)
        }
    }

    private fun evaluateStrategy(strategy: FinalBossStrategy, stats: HeroStats): BossResult {
        return when (strategy) {
            // Single checks - require 120+
            FinalBossStrategy.HAND_TO_HAND -> {
                val martial = stats.getEffectiveMartial()
                BossResult(
                    success = martial >= 120,
                    statResults = mapOf("Martial" to martial),
                    requiredStats = mapOf("Martial" to 120),
                    victoryText = "Your years of training culminate in a legendary duel. You defeat the entity in single combat, becoming a legend yourself.",
                    defeatText = "Your martial prowess, though impressive, is not enough. The entity's power overwhelms you."
                )
            }

            FinalBossStrategy.ARMY -> {
                val nobility = stats.getEffectiveStat(StatType.NOBILITY)
                BossResult(
                    success = nobility >= 120,
                    statResults = mapOf("Nobility" to nobility),
                    requiredStats = mapOf("Nobility" to 120),
                    victoryText = "You rally the kingdoms under your banner. The combined might of all nations drives back the darkness.",
                    defeatText = "Your influence was insufficient. The armies scatter before the entity's terrible power."
                )
            }

            FinalBossStrategy.MERCENARIES -> {
                val financial = stats.getEffectiveStat(StatType.FINANCIAL)
                BossResult(
                    success = financial >= 120,
                    statResults = mapOf("Financial" to financial),
                    requiredStats = mapOf("Financial" to 120),
                    victoryText = "Gold buys loyalty, and loyalty buys victory. Your hired warriors fight with unmatched ferocity, vanquishing the threat.",
                    defeatText = "Even your vast wealth cannot buy victory against such a foe. Your mercenaries flee or fall."
                )
            }

            FinalBossStrategy.MAGIC_SEAL -> {
                val magical = stats.getEffectiveStat(StatType.MAGICAL)
                BossResult(
                    success = magical >= 120,
                    statResults = mapOf("Magical" to magical),
                    requiredStats = mapOf("Magical" to 120),
                    victoryText = "You weave an ancient spell of binding, sealing the entity in an eternal prison beyond reality itself.",
                    defeatText = "Your magic is insufficient. The seal shatters, and the entity breaks free with terrible vengeance."
                )
            }

            FinalBossStrategy.FIREARMS -> {
                val equipment = stats.getEffectiveStat(StatType.EQUIPMENT)
                BossResult(
                    success = equipment >= 120,
                    statResults = mapOf("Equipment" to equipment),
                    requiredStats = mapOf("Equipment" to 120),
                    victoryText = "Modern technology triumphs over ancient evil. Your advanced weaponry tears through the entity's defenses.",
                    defeatText = "Your weapons, though powerful, cannot harm what exists beyond the physical realm."
                )
            }

            FinalBossStrategy.BEFRIEND -> {
                val social = stats.getEffectiveStat(StatType.SOCIAL)
                BossResult(
                    success = social >= 120,
                    statResults = mapOf("Social" to social),
                    requiredStats = mapOf("Social" to 120),
                    victoryText = "Through empathy and understanding, you reach the entity's core. It agrees to depart peacefully, ending the conflict without bloodshed.",
                    defeatText = "Your words fall on deaf ears. The entity has no interest in diplomacy or mercy."
                )
            }

            // Dual checks - require 50+ in BOTH stats
            FinalBossStrategy.MARTIAL_MAGIC -> {
                val martial = stats.getEffectiveMartial()
                val magical = stats.getEffectiveStat(StatType.MAGICAL)
                BossResult(
                    success = martial >= 75 && magical >= 75,
                    statResults = mapOf("Martial" to martial, "Magical" to magical),
                    requiredStats = mapOf("Martial" to 75, "Magical" to 75),
                    victoryText = "You blend blade and spell into a devastating combination. The spellblade technique proves unstoppable.",
                    defeatText = "Your attempt to balance martial and magical arts leaves you master of neither. You are defeated."
                )
            }

            FinalBossStrategy.ARMY_MERCENARIES -> {
                val nobility = stats.getEffectiveStat(StatType.NOBILITY)
                val financial = stats.getEffectiveStat(StatType.FINANCIAL)
                BossResult(
                    success = nobility >= 75 && financial >= 75,
                    statResults = mapOf("Nobility" to nobility, "Financial" to financial),
                    requiredStats = mapOf("Nobility" to 75, "Financial" to 75),
                    victoryText = "You command both noble knights and hired soldiers. This unprecedented alliance overwhelms the entity.",
                    defeatText = "Divided loyalties doom your forces. Nobility and mercenaries refuse to fight together effectively."
                )
            }

            FinalBossStrategy.CEREMONY -> {
                val magical = stats.getEffectiveStat(StatType.MAGICAL)
                val nobility = stats.getEffectiveStat(StatType.NOBILITY)
                BossResult(
                    success = magical >= 75 && nobility >= 75,
                    statResults = mapOf("Magical" to magical, "Nobility" to nobility),
                    requiredStats = mapOf("Magical" to 75, "Nobility" to 75),
                    victoryText = "You perform an ancient royal ritual, channeling the collective will of the kingdom through arcane means. The entity is banished.",
                    defeatText = "The ceremony requires perfect execution. Your incomplete mastery causes it to fail catastrophically."
                )
            }

            FinalBossStrategy.MAGICAL_WEAPONS -> {
                val martial = stats.getEffectiveMartial()
                val equipment = stats.getEffectiveStat(StatType.EQUIPMENT)
                BossResult(
                    success = martial >= 75 && equipment >= 75,
                    statResults = mapOf("Martial" to martial, "Equipment" to equipment),
                    requiredStats = mapOf("Martial" to 75, "Equipment" to 75),
                    victoryText = "Armed with enchanted weapons and the skill to wield them, you strike down the entity in glorious combat.",
                    defeatText = "Magical weapons require both skill and equipment. Your deficiency in one proves fatal."
                )
            }
        }
    }

    private fun showVictoryDialog(strategy: FinalBossStrategy, result: BossResult) {
        // Play victory sound
        AudioManager.playSuccessSound()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🎉 VICTORY! 🎉")
            .setMessage(buildResultMessage(strategy, result, true))
            .setPositiveButton("View Final Story") { _, _ ->
                showFinalStory(true, strategy, result)
            }
            .setCancelable(false)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(Color.parseColor("#4CAF50"))
    }

    private fun showDefeatDialog(strategy: FinalBossStrategy, result: BossResult) {
        // Play defeat sound
        AudioManager.playFailSound()

        val dialog = AlertDialog.Builder(this)
            .setTitle("💀 DEFEAT 💀")
            .setMessage(buildResultMessage(strategy, result, false))
            .setPositiveButton("View Final Story") { _, _ ->
                showFinalStory(false, strategy, result)
            }
            .setCancelable(false)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(Color.parseColor("#F44336"))
    }

    private fun buildResultMessage(strategy: FinalBossStrategy, result: BossResult, victory: Boolean): String {
        val sb = StringBuilder()

        sb.appendLine("Strategy: ${strategy.displayName}\n")

        if (victory) {
            sb.appendLine(result.victoryText)
        } else {
            sb.appendLine(result.defeatText)
        }

        sb.appendLine("\n--- Your Stats ---")
        result.statResults.forEach { (stat, value) ->
            val required = result.requiredStats[stat] ?: 0
            val passed = value >= required
            val symbol = if (passed) "✓" else "✗"
            sb.appendLine("$symbol $stat: $value / $required")
        }

        return sb.toString()
    }

    private fun showFinalStory(victory: Boolean, strategy: FinalBossStrategy, result: BossResult) {
        val finalText = if (victory) {
            buildVictoryStory(strategy, result)
        } else {
            buildDefeatStory(strategy, result)
        }

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            text = finalText
            setPadding(40, 40, 40, 40)
            textSize = 16f
            setTextColor(if (victory) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
        }
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle(if (victory) "⭐ YOUR LEGEND ⭐" else "💀 THE END 💀")
            .setView(scrollView)
            .setPositiveButton("Return to Main Menu") { _, _ ->
                returnToMainMenu()
            }
            .setCancelable(false)
            .show()
    }

    private fun buildVictoryStory(strategy: FinalBossStrategy, result: BossResult): String {
        return buildString {
            appendLine("=== THE HERO'S TRIUMPH ===\n")
            appendLine("After months of trials and challenges, your champion has grown strong enough to face the ultimate evil.\n")

            appendLine("--- THE FINAL BATTLE ---")
            appendLine(result.victoryText)
            appendLine()

            appendLine("--- HOW VICTORY WAS ACHIEVED ---")
            appendLine("Strategy: ${strategy.displayName}")
            appendLine()

            // Show which stats passed
            result.statResults.forEach { (stat, value) ->
                val required = result.requiredStats[stat] ?: 0
                if (value >= required && required > 0) {
                    appendLine("✓ $stat: $value (Required: $required) - PASSED")
                }
            }
            appendLine()

            appendLine("--- THE AFTERMATH ---")
            appendLine("The dark entity is vanquished. Light returns to the realm.")
            appendLine("Your hero's name will echo through the ages.")
            appendLine("Songs will be sung of their courage and sacrifice.")
            appendLine()
            appendLine("As the god who guided them, you watch with pride.")
            appendLine("Your champion has proven worthy.")
            appendLine("The world is saved.")
            appendLine()
            appendLine("But you know... new threats always emerge.")
            appendLine("New heroes will need your guidance.")
            appendLine("The cycle continues.")
            appendLine()
            appendLine("=== YOUR LEGEND IS COMPLETE ===")
        }
    }

    private fun buildDefeatStory(strategy: FinalBossStrategy, result: BossResult): String {
        return buildString {
            appendLine("=== THE HERO'S END ===\n")
            appendLine("Despite months of preparation, your champion was not strong enough.\n")

            appendLine("--- THE FINAL BATTLE ---")
            appendLine(result.defeatText)
            appendLine()

            appendLine("--- WHY THEY FAILED ---")
            appendLine("Strategy Attempted: ${strategy.displayName}")
            appendLine()

            // Show which stats failed
            result.statResults.forEach { (stat, value) ->
                val required = result.requiredStats[stat] ?: 0
                if (required > 0) {
                    val passed = value >= required
                    val symbol = if (passed) "✓" else "✗"
                    appendLine("$symbol $stat: $value (Required: $required) ${if (!passed) "- FAILED" else ""}")
                }
            }
            appendLine()

            appendLine("--- THE AFTERMATH ---")
            appendLine("Darkness spreads across the realm.")
            appendLine("Your champion's sacrifice was valiant, but insufficient.")
            appendLine("The evil entity claims victory.")
            appendLine()
            appendLine("As the god who guided them, you feel the weight of failure.")
            appendLine("Perhaps different choices would have led to victory.")
            appendLine("Perhaps a different path...")
            appendLine()
            appendLine("The world falls into shadow.")
            appendLine()
            appendLine("=== TRY AGAIN ===")
            appendLine("(New Game to attempt a different path)")
        }
    }

    private fun returnToMainMenu() {
        // Stop the boss music and resume the background music
        AudioManager.pauseBossMusic()
        AudioManager.playBackgroundMusic()

        val intent = Intent(this, MainMenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Disable back button during final boss
        // Player must make a choice
    }
}

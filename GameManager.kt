package com.yourgame.herojourney

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class GameManager(private val context: Context, initialStats: HeroStats? = null) {

    companion object {
        private const val TAG = "GameManager"
        private var allNodesCache: Map<String, StoryNode>? = null
    }

    var heroStats: HeroStats = initialStats ?: HeroStats()
    var currentMonth = 0
    val maxMonths = 24 // 2 years

    private val allNodes = mutableMapOf<String, StoryNode>()
    val availableNodes = mutableSetOf<String>()
    val lockedNodes = mutableSetOf<String>()
    val completedNodes = mutableSetOf<String>()
    val choiceHistory = mutableListOf<ChoiceRecord>()

    init {
        loadStoryNodes()
        // Always initialize the node states. For a loaded game, this will be overwritten by the save data.
        initializeNodeStates()
    }

    private fun getCachedNodes(): Map<String, StoryNode>? {
        return allNodesCache
    }

    private fun setCachedNodes(nodes: Map<String, StoryNode>) {
        allNodesCache = nodes
    }


    // ========================================
    // INITIALIZATION
    // ========================================

    private fun loadStoryNodes() {
        val cached = getCachedNodes()
        if (cached != null) {
            allNodes.putAll(cached)
            Log.d(TAG, "✓ Loaded ${allNodes.size} nodes from cache")
            return
        }

        Log.d(TAG, "Cache miss. Loading nodes from JSON...")
        try {
            val jsonString = context.assets.open("story_nodes.json").bufferedReader().use {
                it.readText()
            }

            val type = object : TypeToken<Map<String, List<StoryNode>>>() {}.type
            val data: Map<String, List<StoryNode>> = Gson().fromJson(jsonString, type)

            val nodeList = data["nodes"]
                ?: throw IllegalStateException("JSON missing 'nodes' key")

            nodeList.forEach { node ->
                allNodes[node.id] = node
            }

            // Cache the loaded nodes
            setCachedNodes(allNodes.toMap())

            Log.d(TAG, "✓ Loaded ${allNodes.size} nodes from JSON")

        } catch (e: IOException) {
            Log.e(TAG, "FATAL: Cannot load story_nodes.json", e)
            throw RuntimeException("Game data file missing. Please ensure story_nodes.json exists in assets/", e)
        } catch (e: Exception) {
            Log.e(TAG, "FATAL: Error parsing story_nodes.json", e)
            throw RuntimeException("Game data file corrupted. Please check JSON format.", e)
        }
    }

    /**
     * Initialize which nodes start available vs locked
     */
    private fun initializeNodeStates() {
        allNodes.values.forEach { node ->
            when {
                node.initiallyLocked -> lockedNodes.add(node.id)
                else -> availableNodes.add(node.id)
            }
        }
        Log.d(TAG, "Nodes initialized - Available: ${availableNodes.size}, Locked: ${lockedNodes.size}")
    }

    // ========================================
    // GAME ROUND MANAGEMENT
    // ========================================

    /**
     * Start a new game round
     */
    fun startNewRound(): GameRoundResult {
        currentMonth++

        // Check game over conditions
        val (gameOver, reason) = heroStats.isGameOver()
        if (gameOver) {
            return GameRoundResult.GameOver(reason ?: "Game Over", generateStoryLog())
        }

        // Check for final boss
        if (currentMonth > maxMonths) {
            return GameRoundResult.FinalBoss(heroStats.copy())
        }

        // Select choices with progressive difficulty
        val choices = selectProgressiveChoices()

        // Emergency fallback
        if (choices.isEmpty()) {
            Log.w(TAG, "No choices available - attempting emergency unlock")
            unlockEmergencyNodes()
            val retryChoices = selectProgressiveChoices()

            if (retryChoices.isEmpty()) {
                return GameRoundResult.GameOver("No more paths available.", generateStoryLog())
            }
            return GameRoundResult.Continue(currentMonth, maxMonths, retryChoices, heroStats.copy())
        }

        return GameRoundResult.Continue(currentMonth, maxMonths, choices, heroStats.copy())
    }

    /**
     * Emergency unlock when no choices available
     */
    private fun unlockEmergencyNodes() {
        val toUnlock = lockedNodes.take(3)
        toUnlock.forEach { nodeId ->
            availableNodes.add(nodeId)
            lockedNodes.remove(nodeId)
        }
        Log.w(TAG, "Emergency unlocked ${toUnlock.size} nodes")
    }

    // ========================================
    // CHOICE SELECTION - PROGRESSIVE DIFFICULTY
    // ========================================

    /**
     * NEW: Select choices with difficulty that scales with game progress
     * Early game: More EASY choices
     * Mid game: More MEDIUM choices
     * Late game: More HARD/EXTREME choices
     */
    private fun selectProgressiveChoices(): List<StoryNode> {
        val allAvailable = availableNodes.mapNotNull { allNodes[it] }
        if (allAvailable.isEmpty()) return emptyList()

        // Determine allowed difficulties based on current month
        val allowedDifficulties = when {
            currentMonth <= 5 -> setOf(ChallengeDifficulty.EASY)
            currentMonth <= 10 -> setOf(ChallengeDifficulty.EASY, ChallengeDifficulty.MEDIUM)
            currentMonth <= 15 -> setOf(ChallengeDifficulty.EASY, ChallengeDifficulty.MEDIUM, ChallengeDifficulty.HARD)
            else -> setOf(ChallengeDifficulty.EASY, ChallengeDifficulty.MEDIUM, ChallengeDifficulty.HARD, ChallengeDifficulty.EXTREME)
        }

        Log.d(TAG, "Month $currentMonth - Allowed difficulties: $allowedDifficulties")

        // Filter nodes to only include those where ALL outcomes are within allowed difficulties
        val suitableNodes = allAvailable.filter { node ->
            node.outcomes.all { it.difficulty in allowedDifficulties }
        }

        Log.d(TAG, "Found ${suitableNodes.size} suitable nodes from ${allAvailable.size} available")

        if (suitableNodes.isEmpty()) {
            Log.w(TAG, "No suitable nodes found! Falling back to all available.")
            return allAvailable.shuffled().take(4)
        }

        val finalChoices = mutableListOf<StoryNode>()

        // Group by category for variety
        val byCategory = suitableNodes.groupBy { it.category }
        val shuffledCategories = byCategory.keys.shuffled()

        // Pick one from each category until we have 4 total
        for (category in shuffledCategories) {
            if (finalChoices.size >= 4) break
            byCategory[category]?.randomOrNull()?.let { node ->
                if (node !in finalChoices) {
                    finalChoices.add(node)
                }
            }
        }

        // If we still need more, add random suitable nodes
        if (finalChoices.size < 4) {
            suitableNodes.filter { it !in finalChoices }
                .shuffled()
                .take(4 - finalChoices.size)
                .let { finalChoices.addAll(it) }
        }

        Log.d(TAG, "Selected ${finalChoices.size} choices: ${finalChoices.map { "${it.id} (${it.outcomes.map { o -> o.difficulty }})" }}")

        return finalChoices
    }

    // ========================================
    // CHOICE EXECUTION - HYBRID SYSTEM
    // ========================================

    /**
     * Process player's choice
     */
    fun makeChoice(nodeId: String): ChoiceResult {
        val node = allNodes[nodeId]
            ?: return ChoiceResult.Error("Story node not found: $nodeId")

        // Use HYBRID selection: priority first, then best stat
        val outcome = selectBestOutcomeHybrid(node)
            ?: return ChoiceResult.Error("No valid outcomes for node: ${node.title}")

        val passedCheck = determinePassedCheck(outcome.requirements, heroStats)

        // Execute the choice
        val result = executeChoice(node, outcome, passedCheck)

        // Record in history
        recordChoice(node, outcome)

        return result
    }

    /**
     * HYBRID SYSTEM: Try priority first, then best stat fallback
     */
    /**
     * SIMPLIFIED: Select the best outcome the hero qualifies for
     */
    private fun selectBestOutcomeHybrid(node: StoryNode): Outcome? {
        Log.d(TAG, "Evaluating outcomes for: ${node.id}")
        Log.d(TAG, "Hero stats - M:${heroStats.martial} E:${heroStats.equipment} S:${heroStats.social} F:${heroStats.financial} N:${heroStats.nobility} Mg:${heroStats.magical}")

        // Sort outcomes by priority (lower number = higher priority)
        val sortedOutcomes = node.outcomes.sortedBy { it.priority }

        // Find the first outcome where hero meets ALL requirements
        for (outcome in sortedOutcomes) {
            val meetsReqs = meetsAllRequirements(outcome.requirements, heroStats)

            Log.d(TAG, "  Priority ${outcome.priority}: Requires M:${outcome.requirements.minMartial} E:${outcome.requirements.minEquipment} S:${outcome.requirements.minSocial} F:${outcome.requirements.minFinancial} N:${outcome.requirements.minNobility} Mg:${outcome.requirements.minMagical} - ${if (meetsReqs) "✓ PASS" else "✗ FAIL"}")

            if (meetsReqs) {
                Log.d(TAG, "  → Selected: ${outcome.description.take(50)}...")
                return outcome
            }
        }

        // Should never reach here if you have a proper fallback outcome with priority 99
        Log.e(TAG, "  → ERROR: No outcome matched! Using last outcome as emergency fallback")
        return sortedOutcomes.lastOrNull()
    }

    private fun meetsAllRequirements(reqs: StatRequirements, stats: HeroStats): Boolean {
        return stats.getEffectiveStat(StatType.MARTIAL) >= reqs.minMartial &&
                stats.getEffectiveStat(StatType.EQUIPMENT) >= reqs.minEquipment &&
                stats.getEffectiveStat(StatType.SOCIAL) >= reqs.minSocial &&
                stats.getEffectiveStat(StatType.FINANCIAL) >= reqs.minFinancial &&
                stats.getEffectiveStat(StatType.NOBILITY) >= reqs.minNobility &&
                stats.getEffectiveStat(StatType.MAGICAL) >= reqs.minMagical
    }


    /**
     * Record choice in history
     */
    private fun recordChoice(node: StoryNode, outcome: Outcome) {
        choiceHistory.add(
            ChoiceRecord(
                round = currentMonth,
                nodeId = node.id,
                nodeTitle = node.title,
                outcomeDescription = outcome.description,
                statsSnapshot = heroStats.copy(),
                resultText = outcome.effects.resultText
            )
        )
    }

    // ========================================
    // STAT APPLICATION - WITH JSON BONUSES
    // ========================================

    /**
     * Execute choice and apply all effects
     */
    private fun executeChoice(
        node: StoryNode,
        outcome: Outcome,
        passedCheck: StatType?
    ): ChoiceResult {
        // Mark as completed
        completedNodes.add(node.id)
        availableNodes.remove(node.id)

        // Apply motivation bonus
        applyMotivationBonus()

        // Apply difficulty bonus if check passed
        if (passedCheck != null) {
            applyCheckBonus(passedCheck, outcome.difficulty)
            AudioManager.playSuccessSound()
        }

        // Apply mental state changes (always applied)
        applyMentalStateChanges(outcome.effects)

        // Apply JSON stat bonuses ONLY if check passed
        if (passedCheck != null) {
            applyCheckPassedStatChanges(outcome.effects)
        }

        // Handle node unlocking/locking
        processNodeChanges(outcome)

        return ChoiceResult.Success(outcome, passedCheck, heroStats.copy())
    }

    /**
     * Apply motivation bonus to all stats
     */
    private fun applyMotivationBonus() {
        val bonus = heroStats.motivation / 10
        if (bonus > 0) {
            heroStats.martial += bonus
            heroStats.equipment += bonus
            heroStats.social += bonus
            heroStats.financial += bonus
            heroStats.nobility += bonus
            heroStats.magical += bonus
            Log.d(TAG, "Motivation bonus applied: +$bonus to all stats")
        }
    }

    /**
     * Apply difficulty-based bonus to the stat that passed
     */
    private fun applyCheckBonus(statType: StatType, difficulty: ChallengeDifficulty) {
        val bonus = difficulty.statBonus
        when (statType) {
            StatType.MARTIAL -> heroStats.martial += bonus
            StatType.EQUIPMENT -> heroStats.equipment += bonus
            StatType.SOCIAL -> heroStats.social += bonus
            StatType.FINANCIAL -> heroStats.financial += bonus
            StatType.NOBILITY -> heroStats.nobility += bonus
            StatType.MAGICAL -> heroStats.magical += bonus
        }
        Log.d(TAG, "Check passed bonus: +$bonus to $statType")
    }

    /**
     * Apply motivation, madness, and insight changes
     */
    private fun applyMentalStateChanges(effects: OutcomeEffects) {
        if (effects.motivationChange != 0) {
            heroStats.motivation += effects.motivationChange
            Log.d(TAG, "Motivation changed by ${effects.motivationChange}")
        }
        if (effects.insanityChange != 0) {
            heroStats.madness += effects.insanityChange
            Log.d(TAG, "Madness changed by ${effects.insanityChange}")
        }
        if (effects.insightChange != 0) {
            heroStats.insight += effects.insightChange
            Log.d(TAG, "Insight changed by ${effects.insightChange}")
        }
    }

    /**
     * Apply stat changes from the 'statChanges' map in JSON
     */
    private fun applyCheckPassedStatChanges(effects: OutcomeEffects) {
        effects.statChanges.forEach { (statName, change) ->
            try {
                val statType = StatType.valueOf(statName.uppercase())
                when (statType) {
                    StatType.MARTIAL -> heroStats.martial += change
                    StatType.EQUIPMENT -> heroStats.equipment += change
                    StatType.SOCIAL -> heroStats.social += change
                    StatType.FINANCIAL -> heroStats.financial += change
                    StatType.NOBILITY -> heroStats.nobility += change
                    StatType.MAGICAL -> heroStats.magical += change
                }
                Log.d(TAG, "Applied stat change from JSON: $change to $statType")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Invalid stat name in JSON 'statChanges': $statName")
            }
        }
    }

    /**
     * Unlock and lock nodes based on outcome
     */
    private fun processNodeChanges(outcome: Outcome) {
        outcome.unlocksNodes.forEach { nodeId ->
            if (lockedNodes.contains(nodeId)) {
                lockedNodes.remove(nodeId)
                availableNodes.add(nodeId)
                Log.d(TAG, "Node unlocked: $nodeId")
            }
        }
        outcome.locksNodes.forEach { nodeId ->
            if (availableNodes.contains(nodeId)) {
                availableNodes.remove(nodeId)
                lockedNodes.add(nodeId)
                Log.d(TAG, "Node locked: $nodeId")
            }
        }
    }

    /**
     * Determine if a check passes and which one
     */
    private fun determinePassedCheck(reqs: StatRequirements, stats: HeroStats): StatType? {
        if (stats.getEffectiveStat(StatType.MARTIAL) >= reqs.minMartial && reqs.minMartial > 0) return StatType.MARTIAL
        if (stats.getEffectiveStat(StatType.EQUIPMENT) >= reqs.minEquipment && reqs.minEquipment > 0) return StatType.EQUIPMENT
        if (stats.getEffectiveStat(StatType.SOCIAL) >= reqs.minSocial && reqs.minSocial > 0) return StatType.SOCIAL
        if (stats.getEffectiveStat(StatType.FINANCIAL) >= reqs.minFinancial && reqs.minFinancial > 0) return StatType.FINANCIAL
        if (stats.getEffectiveStat(StatType.NOBILITY) >= reqs.minNobility && reqs.minNobility > 0) return StatType.NOBILITY
        if (stats.getEffectiveStat(StatType.MAGICAL) >= reqs.minMagical && reqs.minMagical > 0) return StatType.MAGICAL
        return null // No check required or passed
    }


    // ========================================
    // UTILITY
    // ========================================

    /**
     * Generate a log of the entire story so far
     */
    fun generateStoryLog(): String {
        if (choiceHistory.isEmpty()) {
            return "The story has not yet begun."
        }

        return buildString {
            appendLine("=== Your Hero's Journey ===")
            appendLine("A tale of adventure over ${currentMonth - 1} months.\n")

            choiceHistory.forEach { record ->
                appendLine("--- Month ${record.round} ---")
                appendLine("Event: ${record.nodeTitle}")
                appendLine("You chose to: ${record.outcomeDescription}")
                appendLine("Result: ${record.resultText}")
                appendLine()
            }

            appendLine("--- Final Stats ---")
            appendLine("Martial: ${heroStats.martial}, Equipment: ${heroStats.equipment}, Social: ${heroStats.social}")
            appendLine("Financial: ${heroStats.financial}, Nobility: ${heroStats.nobility}, Magical: ${heroStats.magical}")
            appendLine("Motivation: ${heroStats.motivation}, Madness: ${heroStats.madness}, Insight: ${heroStats.insight}")
        }
    }
}

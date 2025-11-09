package com.yourgame.herojourney

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameManager(
    private val context: Context,
    initialStats: HeroStats? = null
) {

    companion object {
        private var cachedNodes: Map<String, StoryNode>? = null
        private var lastLoadTime: Long = 0
        private const val CACHE_LIFETIME = 5 * 60 * 1000L
        private const val TAG = "GameManager"
    }

    // Game state
    private val allNodes = mutableMapOf<String, StoryNode>()
    internal val completedNodes = mutableSetOf<String>()
    internal val availableNodes = mutableSetOf<String>()
    internal val lockedNodes = mutableSetOf<String>()

    val heroStats: HeroStats = initialStats?.copy() ?: HeroStats()

    internal var currentMonth = 0
    internal val maxMonths = 20
    internal val choiceHistory = mutableListOf<ChoiceRecord>()

    init {
        loadNodesFromJson()
        initializeNodeStates()
    }

    private fun loadNodesFromJson() {
        val currentTime = System.currentTimeMillis()

        if (cachedNodes != null && (currentTime - lastLoadTime) < CACHE_LIFETIME) {
            Log.d(TAG, "Using cached nodes")
            allNodes.putAll(cachedNodes!!)
            return
        }

        try {
            val jsonString = context.assets.open("story_nodes.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<Map<String, List<StoryNode>>>() {}.type
            val data: Map<String, List<StoryNode>> = Gson().fromJson(jsonString, type)

            data["nodes"]?.forEach { node ->
                allNodes[node.id] = node
            }

            cachedNodes = allNodes.toMap()
            lastLoadTime = currentTime

            Log.d(TAG, "Loaded ${allNodes.size} nodes from JSON")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading nodes", e)
            loadHardcodedNodes()
        }
    }

    private fun loadHardcodedNodes() {
        Log.w(TAG, "Using hardcoded fallback nodes")

        allNodes["training_ground"] = StoryNode(
            id = "training_ground",
            title = "Training Ground",
            description = "Practice your combat skills.",
            category = "combat",
            initiallyLocked = false,
            outcomes = listOf(
                Outcome(
                    description = "Train intensely",
                    requirements = StatRequirements(),
                    effects = OutcomeEffects(
                        motivationChange = 5,
                        insanityChange = 0,
                        insightChange = 2,
                        statChanges = mapOf("martial" to 3),
                        resultText = "You improve your technique."
                    ),
                    difficulty = ChallengeDifficulty.EASY
                )
            )
        )
    }

    private fun initializeNodeStates() {
        allNodes.values.forEach { node ->
            if (node.initiallyLocked) {
                lockedNodes.add(node.id)
            } else {
                availableNodes.add(node.id)
            }
        }
        Log.d(TAG, "Initial - Available: ${availableNodes.size}, Locked: ${lockedNodes.size}")
    }

    fun startNewRound(): GameRoundResult {
        currentMonth++

        val (gameOver, reason) = heroStats.isGameOver()
        if (gameOver) {
            return GameRoundResult.GameOver(reason ?: "Game Over", generateStoryLog())
        }

        if (currentMonth > maxMonths) {
            return processFinalBoss()
        }

        var choices = selectBalancedChoices()

        if (choices.isEmpty()) {
            Log.w(TAG, "No choices were selected. Unlocking emergency nodes.")
            val emergencyNodes = lockedNodes.shuffled().take(2)
            emergencyNodes.forEach {
                availableNodes.add(it)
                lockedNodes.remove(it)
            }
            
            choices = selectBalancedChoices()
            if (choices.isEmpty()) {
                return GameRoundResult.GameOver("No choices available in the entire game.", generateStoryLog())
            }
        }

        return GameRoundResult.Continue(
            month = currentMonth,
            maxMonths = maxMonths,
            availableChoices = choices,
            heroStats = heroStats.copy()
        )
    }

    fun makeChoice(nodeId: String): ChoiceResult {
        val node = allNodes[nodeId] ?: return ChoiceResult.Error("Node not found")

        val outcome = node.outcomes
            .sortedByDescending { it.priority }
            .firstOrNull { meetsRequirements(it.requirements, heroStats) }
            ?: node.outcomes.lastOrNull()
            ?: return ChoiceResult.Error("No outcomes available for node: ${node.id}")

        val passedCheck = determinePassedCheck(outcome.requirements, heroStats)

        val result = executeChoice(node, outcome, passedCheck)

        choiceHistory.add(
            ChoiceRecord(
                round = currentMonth,
                nodeId = nodeId,
                nodeTitle = node.title,
                outcomeDescription = outcome.description,
                statsSnapshot = heroStats.copy(),
                resultText = outcome.effects?.resultText ?: "An uneventful outcome."
            )
        )

        return result
    }

    private fun executeChoice(
        node: StoryNode,
        outcome: Outcome,
        passedCheck: StatType?
    ): ChoiceResult {
        completedNodes.add(node.id)
        availableNodes.remove(node.id)

        val motivationBonus = heroStats.motivation / 10
        heroStats.martial += motivationBonus
        heroStats.equipment += motivationBonus
        heroStats.social += motivationBonus
        heroStats.financial += motivationBonus
        heroStats.nobility += motivationBonus
        heroStats.magical += motivationBonus

        if (passedCheck != null) {
            outcome.difficulty?.let { diff ->
                val checkBonus = diff.statBonus
                when (passedCheck) {
                    StatType.MARTIAL -> heroStats.martial += checkBonus
                    StatType.EQUIPMENT -> heroStats.equipment += checkBonus
                    StatType.SOCIAL -> heroStats.social += checkBonus
                    StatType.FINANCIAL -> heroStats.financial += checkBonus
                    StatType.NOBILITY -> heroStats.nobility += checkBonus
                    StatType.MAGICAL -> heroStats.magical += checkBonus
                }
            }
        }

        outcome.effects?.let { eff ->
            heroStats.motivation += eff.motivationChange
            heroStats.madness += eff.insanityChange
            heroStats.insight += eff.insightChange

            eff.statChanges.forEach { (stat, change) ->
                when (stat.lowercase()) {
                    "martial" -> heroStats.martial += change
                    "equipment" -> heroStats.equipment += change
                    "social" -> heroStats.social += change
                    "financial" -> heroStats.financial += change
                    "nobility" -> heroStats.nobility += change
                    "magical" -> heroStats.magical += change
                }
            }
        }

        outcome.unlocksNodes?.forEach { newNodeId ->
            if (allNodes.containsKey(newNodeId) && !completedNodes.contains(newNodeId)) {
                lockedNodes.remove(newNodeId)
                availableNodes.add(newNodeId)
            }
        }

        outcome.locksNodes?.forEach { lockedNodeId ->
            availableNodes.remove(lockedNodeId)
            if (!completedNodes.contains(lockedNodeId)) {
                lockedNodes.add(lockedNodeId)
            }
        }

        return ChoiceResult.Success(outcome, passedCheck, heroStats.copy())
    }

    private fun meetsRequirements(reqs: StatRequirements?, stats: HeroStats): Boolean {
        if (reqs == null) return true
        return stats.getEffectiveStat(StatType.MARTIAL) >= reqs.minMartial &&
                stats.getEffectiveStat(StatType.EQUIPMENT) >= reqs.minEquipment &&
                stats.getEffectiveStat(StatType.SOCIAL) >= reqs.minSocial &&
                stats.getEffectiveStat(StatType.FINANCIAL) >= reqs.minFinancial &&
                stats.getEffectiveStat(StatType.NOBILITY) >= reqs.minNobility &&
                stats.getEffectiveStat(StatType.MAGICAL) >= reqs.minMagical
    }

    private fun determinePassedCheck(reqs: StatRequirements?, stats: HeroStats): StatType? {
        if (reqs == null) return null
        val checks = listOf(
            StatType.MARTIAL to reqs.minMartial,
            StatType.EQUIPMENT to reqs.minEquipment,
            StatType.SOCIAL to reqs.minSocial,
            StatType.FINANCIAL to reqs.minFinancial,
            StatType.NOBILITY to reqs.minNobility,
            StatType.MAGICAL to reqs.minMagical
        )

        return checks
            .filter { (statType, requiredValue) -> requiredValue > 0 && stats.getEffectiveStat(statType) >= requiredValue }
            .maxByOrNull { (_, requiredValue) -> requiredValue }
            ?.first
    }

    private fun selectBalancedChoices(): List<StoryNode> {
        val allAvailable = availableNodes.mapNotNull { allNodes[it] }
        if (allAvailable.isEmpty()) return emptyList()

        // 1. Group by category
        val byCategory = allAvailable.groupBy { it.category }

        // 2. Try to pick one from each of 4 different categories
        val finalChoices = mutableListOf<StoryNode>()
        val shuffledCategories = byCategory.keys.shuffled().toMutableList()

        while (finalChoices.size < 4 && shuffledCategories.isNotEmpty()) {
            val category = shuffledCategories.removeFirst()
            byCategory[category]?.shuffled()?.firstOrNull()?.let {
                finalChoices.add(it)
            }
        }

        // 3. If we still don't have 4, fill the rest with random choices from the remaining pool
        if (finalChoices.size < 4) {
            val remaining = allAvailable.filter { it !in finalChoices }
            finalChoices.addAll(remaining.shuffled().take(4 - finalChoices.size))
        }

        return finalChoices.distinct().shuffled()
    }

    private fun processFinalBoss(): GameRoundResult {
        val allStats = listOf(
            heroStats.martial,
            heroStats.equipment,
            heroStats.social,
            heroStats.financial,
            heroStats.nobility,
            heroStats.magical
        )

        val victory = when {
            allStats.all { it >= 40 } -> true
            allStats.any { it >= 80 } -> true
            allStats.count { it >= 60 } >= 3 -> true
            else -> false
        }

        val message = if (victory) {
            "Victory! You defeated the final enemy!"
        } else {
            "Defeat. You were not strong enough."
        }

        return GameRoundResult.GameOver(message, generateStoryLog())
    }

    fun generateStoryLog(): String {
        val sb = StringBuilder()

        sb.appendLine("=== HERO'S JOURNEY ===\n")
        sb.appendLine("Duration: $currentMonth months")
        sb.appendLine("Challenges: ${choiceHistory.size}\n")

        sb.appendLine("Final Stats:")
        sb.appendLine("  Martial: ${heroStats.martial}")
        sb.appendLine("  Equipment: ${heroStats.equipment}")
        sb.appendLine("  Social: ${heroStats.social}")
        sb.appendLine("  Financial: ${heroStats.financial}")
        sb.appendLine("  Nobility: ${heroStats.nobility}")
        sb.appendLine("  Magical: ${heroStats.magical}")
        sb.appendLine("  Motivation: ${heroStats.motivation}")
        sb.appendLine("  Madness: ${heroStats.madness}")
        sb.appendLine("  Insight: ${heroStats.insight}\n")

        sb.appendLine("=== CHRONICLE ===\n")

        choiceHistory.forEach { record ->
            sb.appendLine("Month ${record.round}: ${record.nodeTitle}")
            sb.appendLine("  ${record.outcomeDescription}")
            sb.appendLine("  ${record.resultText}")
            sb.appendLine()
        }

        return sb.toString()
    }

    fun analyzeBalance(): String {
        val nodes = allNodes.values.toList()

        val avgMotivation = nodes.flatMap { it.outcomes }
            .mapNotNull { it.effects?.motivationChange }
            .average()

        val avgMadness = nodes.flatMap { it.outcomes }
            .mapNotNull { it.effects?.insanityChange }
            .average()

        return buildString {
            appendLine("=== BALANCE ANALYSIS ===")
            appendLine("Total Nodes: ${nodes.size}")
            appendLine("Available: ${availableNodes.size}")
            appendLine("Locked: ${lockedNodes.size}")
            appendLine()
            appendLine("Avg Motivation/Round: ${String.format("%.2f", avgMotivation)}")
            appendLine("Avg Madness/Round: ${String.format("%.2f", avgMadness)}")
        }
    }
}

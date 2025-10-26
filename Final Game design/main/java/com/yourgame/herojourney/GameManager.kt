package com.yourgame.herojourney

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GameManager(private val context: Context) {
    internal val allNodes = mutableMapOf<String, StoryNode>()
    internal val completedNodes = mutableSetOf<String>()
    internal val availableNodes = mutableSetOf<String>()
    internal val lockedNodes = mutableSetOf<String>()

    internal val choiceHistory = mutableListOf<ChoiceRecord>()
    internal var currentMonth = 0
    internal val maxMonths = 20

    val heroStats = HeroStats()

    init {
        loadNodesFromJson()
        initializeAvailableNodes()
        Log.d("GameManager", "Initialized with ${allNodes.size} total nodes")
        Log.d("GameManager", "Available nodes: ${availableNodes.size}")
    }

    private fun loadNodesFromJson() {
        try {
            val jsonString = context.assets.open("story_nodes.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<Map<String, List<StoryNode>>>() {}.type
            val data: Map<String, List<StoryNode>> = Gson().fromJson(jsonString, type)

            val validatedNodes = data["nodes"]?.filter { node ->
                if (node.outcomes.isNullOrEmpty()) {
                    Log.w("GameManager", "Skipping node '${node.id}' because it has no 'outcomes'.")
                    return@filter false
                }

                val hasInvalidOutcomes = node.outcomes.any { outcome ->
                    (outcome.difficulty == null || outcome.effects?.resultText == null)
                }

                if (hasInvalidOutcomes) {
                    Log.w("GameManager", "Skipping node '${node.id}' due to incomplete outcome data.")
                    return@filter false
                }
                true
            } ?: emptyList()

            validatedNodes.forEach { node ->
                allNodes[node.id] = node
            }

            Log.d("GameManager", "Successfully loaded ${allNodes.size} nodes from JSON")

        } catch (e: Exception) {
            Log.e("GameManager", "Failed to load or parse story_nodes.json", e)
            loadHardcodedNodes()
        }
    }

    private fun loadHardcodedNodes() {
        Log.d("GameManager", "Loading hardcoded fallback nodes")
        allNodes["training_ground"] = StoryNode(
            id = "training_ground",
            title = "Training Ground",
            description = "Practice your combat skills at the training yard.",
            category = "combat",
            outcomes = listOf(
                Outcome(
                    description = "Train intensely",
                    requirements = StatRequirements(),
                    effects = OutcomeEffects(
                        motivationChange = 5,
                        insanityChange = 0,
                        insightChange = 2,
                        statChanges = mapOf("martial" to 3),
                        resultText = "You improve your fighting technique."
                    ),
                    difficulty = ChallengeDifficulty.EASY
                )
            )
        )
    }

    private fun initializeAvailableNodes() {
        // Start with ALL nodes available
        availableNodes.addAll(allNodes.keys)
        Log.d("GameManager", "Initialized ${availableNodes.size} available nodes")
    }

    fun startNewRound(): GameRoundResult {
        currentMonth++

        val (gameOver, reason) = heroStats.isGameOver()
        if (gameOver) {
            return GameRoundResult.GameOver(reason ?: "Unknown reason", generateStoryLog())
        }

        if (currentMonth > maxMonths) {
            return processFinalBoss()
        }

        val choices = selectBalancedChoices()

        Log.d("GameManager", "Round $currentMonth: Selected ${choices.size} choices")

        if (choices.isEmpty()) {
            return GameRoundResult.GameOver("No more choices available", generateStoryLog())
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

        if (node.outcomes.isNullOrEmpty()) {
            return ChoiceResult.Error("Node '${node.id}' has no outcomes.")
        }

        val outcome = node.outcomes
            .sortedByDescending { it.priority }
            .firstOrNull { meetsRequirements(it.requirements, heroStats) }
            ?: node.outcomes.last()

        val passedCheck = determinePassedCheck(outcome.requirements, heroStats)

        val result = executeChoice(node, outcome, passedCheck)

        choiceHistory.add(
            ChoiceRecord(
                round = currentMonth,
                nodeId = nodeId,
                nodeTitle = node.title,
                outcomeDescription = outcome.description,
                statsSnapshot = heroStats.copy(),
                resultText = outcome.effects?.resultText ?: "An unknown outcome occurred."
            )
        )

        if (result is ChoiceResult.Success) {
            SaveManager(context).autoSave(this)
        }

        return result
    }

    private fun executeChoice(
        node: StoryNode,
        outcome: Outcome,
        passedCheck: StatType?
    ): ChoiceResult {
        // Mark as completed (for tracking, but don't remove from available)
        completedNodes.add(node.id)

        // DON'T remove from availableNodes - we want to reuse nodes!
        // ONLY remove if it's a tragedy or should be one-time
        if (node.category == "tragedy") {
            availableNodes.remove(node.id)
            Log.d("GameManager", "Removed tragedy node '${node.id}' from available pool")
        }

        // Apply motivation bonus to all stats
        val motivationBonus = heroStats.motivation / 10
        heroStats.martial += motivationBonus
        heroStats.equipment += motivationBonus
        heroStats.social += motivationBonus
        heroStats.financial += motivationBonus
        heroStats.nobility += motivationBonus
        heroStats.magical += motivationBonus

        // Apply check bonus if passed
        if (passedCheck != null) {
            outcome.difficulty?.statBonus?.let { checkBonus ->
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

        // Apply outcome effects
        outcome.effects?.let {
            heroStats.motivation += it.motivationChange
            heroStats.madness += it.insanityChange
            heroStats.insight += it.insightChange

            it.statChanges.forEach { (stat, change) ->
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

        // Cap stats at 0-100
        heroStats.motivation = heroStats.motivation.coerceIn(0, 100)
        heroStats.madness = heroStats.madness.coerceIn(0, 100)
        heroStats.insight = heroStats.insight.coerceIn(0, 100)

        // Handle unlocking nodes
        outcome.unlocksNodes.forEach { newNodeId ->
            if (newNodeId in allNodes && newNodeId !in lockedNodes) {
                availableNodes.add(newNodeId)
                Log.d("GameManager", "Unlocked node: $newNodeId")
            }
        }

        // Handle locking nodes
        outcome.locksNodes.forEach { lockedNodeId ->
            availableNodes.remove(lockedNodeId)
            lockedNodes.add(lockedNodeId)
            Log.d("GameManager", "Locked node: $lockedNodeId")
        }

        return ChoiceResult.Success(
            outcome = outcome,
            passedCheck = passedCheck,
            updatedStats = heroStats.copy()
        )
    }

    private fun meetsRequirements(reqs: StatRequirements, stats: HeroStats): Boolean {
        return stats.getEffectiveStat(StatType.MARTIAL) >= reqs.minMartial &&
                stats.getEffectiveStat(StatType.EQUIPMENT) >= reqs.minEquipment &&
                stats.getEffectiveStat(StatType.SOCIAL) >= reqs.minSocial &&
                stats.getEffectiveStat(StatType.FINANCIAL) >= reqs.minFinancial &&
                stats.getEffectiveStat(StatType.NOBILITY) >= reqs.minNobility &&
                stats.getEffectiveStat(StatType.MAGICAL) >= reqs.minMagical
    }

    private fun determinePassedCheck(reqs: StatRequirements, stats: HeroStats): StatType? {
        val checks = listOf(
            StatType.MARTIAL to reqs.minMartial,
            StatType.EQUIPMENT to reqs.minEquipment,
            StatType.SOCIAL to reqs.minSocial,
            StatType.FINANCIAL to reqs.minFinancial,
            StatType.NOBILITY to reqs.minNobility,
            StatType.MAGICAL to reqs.minMagical
        )

        return checks
            .filter { (type, req) -> req > 0 && stats.getEffectiveStat(type) >= req }
            .maxByOrNull { (_, req) -> req }
            ?.first
    }

    private fun selectBalancedChoices(): List<StoryNode> {
        // Get all available nodes
        val available = availableNodes.mapNotNull { allNodes[it] }

        Log.d("GameManager", "Available nodes pool: ${available.size}")

        if (available.isEmpty()) {
            Log.e("GameManager", "No available nodes!")
            return emptyList()
        }

        // Get recently played nodes (last 3 choices)
        val recentNodeIds = choiceHistory.takeLast(3).map { it.nodeId }.toSet()

        // Filter out recently played nodes to avoid immediate repetition
        val notRecent = available.filter { it.id !in recentNodeIds }

        // Use notRecent if we have enough, otherwise use all available
        val candidatePool = if (notRecent.size >= 4) notRecent else available

        Log.d("GameManager", "Candidate pool size: ${candidatePool.size}")

        // Try to get diverse categories
        val recentCategories = choiceHistory
            .takeLast(3)
            .map { allNodes[it.nodeId]?.category }
            .filterNotNull()
            .toSet()

        val diverse = candidatePool.filter { it.category !in recentCategories }

        val selected = when {
            // If we have 4+ diverse options, pick from different categories
            diverse.size >= 4 -> {
                diverse.groupBy { it.category }
                    .values
                    .shuffled()
                    .take(4)
                    .mapNotNull { it.randomOrNull() }
            }
            // Otherwise just pick 4 random from candidate pool
            candidatePool.size >= 4 -> {
                candidatePool.shuffled().take(4)
            }
            // If we have less than 4, return what we have
            else -> {
                candidatePool.shuffled()
            }
        }

        Log.d("GameManager", "Selected ${selected.size} choices: ${selected.map { it.id }}")

        return selected
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
            "Victory! The hero has overcome all challenges and defeated the final enemy!"
        } else {
            "Defeat. The hero was not strong enough to face the final challenge."
        }

        return GameRoundResult.GameOver(message, generateStoryLog())
    }

    internal fun generateStoryLog(): String {
        val sb = StringBuilder()
        sb.appendLine("=== HERO'S JOURNEY ===\n")
        sb.appendLine("Duration: $currentMonth months")
        sb.appendLine("Challenges Faced: ${choiceHistory.size}\n")

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

        sb.appendLine("=== CHRONICLE OF EVENTS ===\n")

        choiceHistory.forEachIndexed { index, record ->
            sb.appendLine("Month ${record.round}: ${record.nodeTitle}")
            sb.appendLine("  Choice: ${record.outcomeDescription}")
            sb.appendLine("  Result: ${record.resultText}")
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

        val expectedTotalMotivation = avgMotivation * maxMonths
        val expectedTotalMadness = avgMadness * maxMonths

        val sb = StringBuilder()
        sb.appendLine("=== GAME BALANCE ANALYSIS ===")
        sb.appendLine("Total Nodes: ${nodes.size}")
        sb.appendLine("Max Months: $maxMonths")
        sb.appendLine()
        sb.appendLine("Average Motivation Change: $avgMotivation")
        sb.appendLine("Average Madness Change: $avgMadness")
        sb.appendLine()
        sb.appendLine("Expected after $maxMonths months:")
        sb.appendLine("  Total Motivation: +$expectedTotalMotivation")
        sb.appendLine("  Total Madness: +$expectedTotalMadness")
        sb.appendLine()

        if (expectedTotalMadness >= 100) {
            sb.appendLine("⚠️ WARNING: Game is too hard! Average player will go insane.")
        } else if (expectedTotalMadness < 30) {
            sb.appendLine("⚠️ WARNING: Game may be too easy! Not enough tension.")
        } else {
            sb.appendLine("✓ Balance looks good!")
        }

        return sb.toString()
    }
}
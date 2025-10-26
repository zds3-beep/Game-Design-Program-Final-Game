package com.yourgame.herojourney

data class HeroStats(
    var martial: Int = 10,
    var equipment: Int = 10,
    var social: Int = 10,
    var financial: Int = 10,
    var nobility: Int = 10,
    var magical: Int = 10,
    var motivation: Int = 50,
    var madness: Int = 0,
    var insight: Int = 0
) {
    fun getEffectiveMartial(): Int {
        val madnessBonus = if (madness > 50) madness / 10 else 0
        return martial + (insight / 10) + madnessBonus
    }

    fun getEffectiveStat(statType: StatType): Int {
        val baseValue = when (statType) {
            StatType.MARTIAL -> martial
            StatType.EQUIPMENT -> equipment
            StatType.SOCIAL -> social
            StatType.FINANCIAL -> financial
            StatType.NOBILITY -> nobility
            StatType.MAGICAL -> magical
        }

        val insightBonus = insight / 10

        val madnessMod = if (madness > 50) {
            when (statType) {
                StatType.MARTIAL -> madness / 10
                StatType.SOCIAL, StatType.FINANCIAL,
                StatType.NOBILITY, StatType.EQUIPMENT -> -(madness / 10)
                StatType.MAGICAL -> 0
            }
        } else 0

        return baseValue + insightBonus + madnessMod
    }

    fun isGameOver(): Pair<Boolean, String?> {
        if (madness >= 100) return true to "Hero succumbed to madness"
        if (insight >= 100) return true to "Hero gained self-awareness and retired"
        return false to null
    }
}

enum class StatType {
    MARTIAL, EQUIPMENT, SOCIAL, FINANCIAL, NOBILITY, MAGICAL
}

data class StatRequirements(
    val minMartial: Int = 0,
    val minEquipment: Int = 0,
    val minSocial: Int = 0,
    val minFinancial: Int = 0,
    val minNobility: Int = 0,
    val minMagical: Int = 0
)

data class OutcomeEffects(
    val motivationChange: Int = 0,
    val insanityChange: Int = 0,
    val insightChange: Int = 0,
    val statChanges: Map<String, Int> = emptyMap(),
    val resultText: String
)

data class Outcome(
    val description: String,
    val requirements: StatRequirements,
    val effects: OutcomeEffects,
    val unlocksNodes: List<String> = emptyList(),
    val locksNodes: List<String> = emptyList(),
    val priority: Int = 0,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM
)

enum class ChallengeDifficulty(val statBonus: Int) {
    EASY(3),
    MEDIUM(5),
    HARD(8),
    EXTREME(12)
}

data class StoryNode(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val outcomes: List<Outcome>
)

data class ChoiceRecord(
    val round: Int,
    val nodeId: String,
    val nodeTitle: String,
    val outcomeDescription: String,
    val statsSnapshot: HeroStats,
    val resultText: String
)

sealed class GameRoundResult {
    data class Continue(
        val month: Int,
        val maxMonths: Int,
        val availableChoices: List<StoryNode>,
        val heroStats: HeroStats
    ) : GameRoundResult()

    data class GameOver(val message: String, val storyLog: String) : GameRoundResult()
}

sealed class ChoiceResult {
    data class Success(
        val outcome: Outcome,
        val passedCheck: StatType?,
        val updatedStats: HeroStats
    ) : ChoiceResult()

    data class Error(val message: String) : ChoiceResult()
}
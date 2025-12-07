package com.yourgame.herojourney

// ========================================
// HERO STATS - Main character statistics
// ========================================

data class HeroStats(
    // Power Stats (0-999)
    private var _martial: Int = 10,
    private var _equipment: Int = 10,
    private var _social: Int = 10,
    private var _financial: Int = 10,
    private var _nobility: Int = 10,
    private var _magical: Int = 10,

    // Mental State Bars (0-100)
    private var _motivation: Int = 50,
    private var _madness: Int = 0,
    private var _insight: Int = 0
) {
    // Public properties with automatic bounds checking
    var martial: Int
        get() = _martial
        set(value) { _martial = value.coerceIn(0, 999) }

    var equipment: Int
        get() = _equipment
        set(value) { _equipment = value.coerceIn(0, 999) }

    var social: Int
        get() = _social
        set(value) { _social = value.coerceIn(0, 999) }

    var financial: Int
        get() = _financial
        set(value) { _financial = value.coerceIn(0, 999) }

    var nobility: Int
        get() = _nobility
        set(value) { _nobility = value.coerceIn(0, 999) }

    var magical: Int
        get() = _magical
        set(value) { _magical = value.coerceIn(0, 999) }

    var motivation: Int
        get() = _motivation
        set(value) { _motivation = value.coerceIn(0, 100) }

    var madness: Int
        get() = _madness
        set(value) { _madness = value.coerceIn(0, 100) }

    var insight: Int
        get() = _insight
        set(value) { _insight = value.coerceIn(0, 100) }

    // Calculate effective stats with all modifiers
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

        // Madness modifiers (only apply if madness > 50)
        val madnessMod = if (madness > 50) {
            when (statType) {
                StatType.MARTIAL -> madness / 10          // Bonus to martial
                StatType.SOCIAL, StatType.FINANCIAL,
                StatType.NOBILITY, StatType.EQUIPMENT -> -(madness / 10)  // Penalty to others
                StatType.MAGICAL -> 0                      // No effect on magical
            }
        } else 0

        // CRITICAL: Ensure result never goes negative
        return (baseValue + insightBonus + madnessMod).coerceAtLeast(0)
    }

    fun getEffectiveMartial(): Int = getEffectiveStat(StatType.MARTIAL)

    // Check if game should end
    fun isGameOver(): Pair<Boolean, String?> {
        if (madness >= 100) return true to "Hero succumbed to madness"
        if (insight >= 100) return true to "Hero gained self-awareness and retired"
        return false to null
    }

    // Create a copy of stats
    fun copy(): HeroStats {
        return HeroStats(
            _martial = this._martial,
            _equipment = this._equipment,
            _social = this._social,
            _financial = this._financial,
            _nobility = this._nobility,
            _magical = this._magical,
            _motivation = this._motivation,
            _madness = this._madness,
            _insight = this._insight
        )
    }
}


// ========================================
// STAT TYPE ENUM
// ========================================

enum class StatType {
    MARTIAL,
    EQUIPMENT,
    SOCIAL,
    FINANCIAL,
    NOBILITY,
    MAGICAL
}


// ========================================
// STAT REQUIREMENTS
// ========================================

data class StatRequirements(
    val minMartial: Int = 0,
    val minEquipment: Int = 0,
    val minSocial: Int = 0,
    val minFinancial: Int = 0,
    val minNobility: Int = 0,
    val minMagical: Int = 0
)


// ========================================
// OUTCOME EFFECTS
// ========================================

data class OutcomeEffects(
    val motivationChange: Int = 0,
    val insanityChange: Int = 0,
    val insightChange: Int = 0,
    val statChanges: Map<String, Int> = emptyMap(),
    val resultText: String
)


// ========================================
// CHALLENGE DIFFICULTY
// ========================================

enum class ChallengeDifficulty(val statBonus: Int) {
    EASY(3),
    MEDIUM(5),
    HARD(8),
    EXTREME(12)
}


// ========================================
// OUTCOME
// ========================================

data class Outcome(
    val description: String,
    val requirements: StatRequirements,
    val effects: OutcomeEffects,
    val unlocksNodes: List<String> = emptyList(),
    val locksNodes: List<String> = emptyList(),
    val priority: Int = 0,
    val difficulty: ChallengeDifficulty = ChallengeDifficulty.MEDIUM
)


// ========================================
// STORY NODE
// ========================================

data class StoryNode(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val initiallyLocked: Boolean = false,  // NEW: For quest chains
    val outcomes: List<Outcome>
)


// ========================================
// CHOICE RECORD (For history/log)
// ========================================

data class ChoiceRecord(
    val round: Int,
    val nodeId: String,
    val nodeTitle: String,
    val outcomeDescription: String,
    val statsSnapshot: HeroStats,
    val resultText: String
)

// ========================================
// GAME ROUND RESULT
// ========================================

sealed class GameRoundResult {
    data class Continue(
        val month: Int,
        val maxMonths: Int,
        val availableChoices: List<StoryNode>,
        val heroStats: HeroStats
    ) : GameRoundResult()

    data class GameOver(
        val message: String,
        val storyLog: String
    ) : GameRoundResult()

    data class FinalBoss(
        val heroStats: HeroStats
    ) : GameRoundResult()
}


// ========================================
// CHOICE RESULT
// ========================================

sealed class ChoiceResult {
    data class Success(
        val outcome: Outcome,
        val passedCheck: StatType?,
        val updatedStats: HeroStats
    ) : ChoiceResult()

    data class Error(val message: String) : ChoiceResult()
}


// ========================================
// BACKGROUND SYSTEM
// ========================================

data class HeroBackground(
    val id: String,
    val name: String,
    val description: String,
    val flavorText: String,
    val initialStats: HeroStats,
    val difficulty: BackgroundDifficulty
)

enum class BackgroundDifficulty(val displayName: String, val color: Int) {
    VERY_HARD("Very Hard", 0xFFD32F2F.toInt()),
    HARD("Hard", 0xFFF57C00.toInt()),
    MEDIUM("Medium", 0xFFFDD835.toInt()),
    EASY("Easy", 0xFF66BB6A.toInt())
}


// ========================================
// SAVE SYSTEM
// ========================================

data class GameSaveData(
    val saveDate: Long = System.currentTimeMillis(),
    val saveName: String = "",
    val backgroundId: String = "humble_origins",

    // Hero Stats
    val heroStats: HeroStats,

    // Game Progress
    val currentMonth: Int,
    val maxMonths: Int,

    // Node Tracking
    val completedNodeIds: List<String>,
    val availableNodeIds: List<String>,
    val lockedNodeIds: List<String>,

    // Choice History
    val choiceHistory: List<ChoiceRecord>,

    // Metadata
    val totalPlayTime: Long = 0L
)

data class SaveFileMetadata(
    val fileName: String,
    val saveName: String,
    val saveDate: Long,
    val currentMonth: Int,
    val heroLevel: Int,
    val backgroundName: String = "Unknown"
)

// ========================================
// FINAL BOSS SYSTEM
// ========================================

enum class FinalBossStrategy(val displayName: String) {
    // Single stat checks
    HAND_TO_HAND("Hand-to-Hand Combat"),
    ARMY("Lead an Army"),
    MERCENARIES("Hire Mercenaries"),
    MAGIC_SEAL("Seal with Magic"),
    FIREARMS("Modern Firearms"),
    BEFRIEND("Befriend the Entity"),

    // Dual stat checks
    MARTIAL_MAGIC("Martial Arts & Magic"),
    ARMY_MERCENARIES("Army & Mercenaries"),
    CEREMONY("Royal Ceremony"),
    MAGICAL_WEAPONS("Magical Weapons")
}

data class BossResult(
    val success: Boolean,
    val statResults: Map<String, Int>,
    val requiredStats: Map<String, Int>,
    val victoryText: String,
    val defeatText: String
)
package com.yourgame.herojourney

object BackgroundRepository {

    fun getAllBackgrounds(): List<HeroBackground> {
        return listOf(
            HeroBackground(
                id = "humble_origins",
                name = "Humble Origins",
                description = "Born to nothing, destined for greatness through sheer willpower.",
                flavorText = "You have no advantages, only determination. Every victory will be hard-earned.",
                initialStats = HeroStats(),
                difficulty = BackgroundDifficulty.VERY_HARD
            ),

            HeroBackground(
                id = "child_soldier",
                name = "Child Soldier",
                description = "Forged in battle, scarred by war.",
                flavorText = "You know only combat. Your past haunts you, but your blade is sharp.",
                initialStats = HeroStats(
                    _martial = 25,
                    _equipment = 10,
                    _social = 1,
                    _financial = 10,
                    _nobility = 1,
                    _magical = 1,
                    _motivation = 25,
                    _madness = 25,
                    _insight = 0
                ),
                difficulty = BackgroundDifficulty.EASY
            ),

            HeroBackground(
                id = "child_of_noble",
                name = "Child of a Noble",
                description = "Born into privilege and power.",
                flavorText = "You have resources and connections, but lack combat experience.",
                initialStats = HeroStats(
                    _martial = 10,
                    _equipment = 15,
                    _social = 15,
                    _financial = 25,
                    _nobility = 15,
                    _magical = 1,
                    _motivation = 0,
                    _madness = 10,
                    _insight = 0
                ),
                difficulty = BackgroundDifficulty.VERY_HARD
            ),

            HeroBackground(
                id = "dreamer_farm_child",
                name = "Dreamer Farm Child",
                description = "A simple life filled with big dreams.",
                flavorText = "You grew up listening to tales of heroes. Now it's your turn to become one.",
                initialStats = HeroStats(
                    _martial = 1,
                    _equipment = 1,
                    _social = 20,
                    _financial = 1,
                    _nobility = 1,
                    _magical = 5,
                    _motivation = 25,
                    _madness = 0,
                    _insight = 10
                ),
                difficulty = BackgroundDifficulty.EASY
            ),

            HeroBackground(
                id = "city_urchin",
                name = "City Urchin",
                description = "Survived the streets through wit and charm.",
                flavorText = "The streets taught you to read people and survive by your wits.",
                initialStats = HeroStats(
                    _martial = 5,
                    _equipment = 5,
                    _social = 25,
                    _financial = 5,
                    _nobility = 1,
                    _magical = 1,
                    _motivation = 25,
                    _madness = 10,
                    _insight = 10
                ),
                difficulty = BackgroundDifficulty.EASY
            ),

            HeroBackground(
                id = "dignitary_child",
                name = "Dignitary Child",
                description = "Raised in the halls of power and diplomacy.",
                flavorText = "You excel at negotiation and politics, but the battlefield is foreign to you.",
                initialStats = HeroStats(
                    _martial = 10,
                    _equipment = 15,
                    _social = 25,
                    _financial = 10,
                    _nobility = 10,
                    _magical = 1,
                    _motivation = 0,
                    _madness = 0,
                    _insight = 0
                ),
                difficulty = BackgroundDifficulty.VERY_HARD
            ),

            HeroBackground(
                id = "magical_academy_orphan",
                name = "Magical Academy Orphan",
                description = "Abandoned at a magical academy, raised among ancient tomes.",
                flavorText = "Magic flows through you, but the world outside the academy is strange and dangerous.",
                initialStats = HeroStats(
                    _martial = 1,
                    _equipment = 10,
                    _social = 1,
                    _financial = 5,
                    _nobility = 5,
                    _magical = 25,
                    _motivation = 15,
                    _madness = 10,
                    _insight = 20
                ),
                difficulty = BackgroundDifficulty.MEDIUM
            ),

            HeroBackground(
                id = "tinkers_child",
                name = "Tinker's Child",
                description = "Grew up in a workshop surrounded by gears and inventions.",
                flavorText = "You understand machines and commerce, but lack combat prowess.",
                initialStats = HeroStats(
                    _martial = 1,
                    _equipment = 25,
                    _social = 1,
                    _financial = 25,
                    _nobility = 10,
                    _magical = 15,
                    _motivation = 10,
                    _madness = 0,
                    _insight = 10
                ),
                difficulty = BackgroundDifficulty.MEDIUM
            )
        )
    }

    fun getBackgroundById(id: String): HeroBackground? {
        return getAllBackgrounds().find { it.id == id }
    }
}
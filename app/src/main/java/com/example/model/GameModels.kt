package com.example.model

import androidx.compose.ui.graphics.Color

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class DesertTimeOfDay(val displayName: String) {
    DAY("Day Desert"),
    SUNSET("Sunset Desert"),
    NIGHT("Starry Night")
}

enum class BirdSkin(
    val id: String,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val description: String
) {
    SCARLET_FINCH(
        id = "scarlet_finch",
        displayName = "Scarlet Finch",
        primaryColor = Color(0xFFE53935),
        secondaryColor = Color(0xFFC62828),
        accentColor = Color(0xFFFFB300),
        description = "Classic spirited red desert bird"
    ),
    DESERT_PHOENIX(
        id = "desert_phoenix",
        displayName = "Desert Phoenix",
        primaryColor = Color(0xFFFF3D00),
        secondaryColor = Color(0xFFBF360C),
        accentColor = Color(0xFFFFD54F),
        description = "Fiery red with golden solar feathers"
    ),
    CRIMSON_FALCON(
        id = "crimson_falcon",
        displayName = "Crimson Falcon",
        primaryColor = Color(0xFFB71C1C),
        secondaryColor = Color(0xFF880E4F),
        accentColor = Color(0xFFFF8A80),
        description = "Sleek deep-ruby desert hunter"
    )
}

enum class MedalType(val title: String, val minScore: Int, val color: Color) {
    NONE("No Medal", 0, Color.Transparent),
    BRONZE("Bronze Cactus", 10, Color(0xFFCD7F32)),
    SILVER("Silver Dune", 20, Color(0xFFC0C0C0)),
    GOLD("Golden Sun", 35, Color(0xFFFFD700)),
    RUBY("Ruby Legend", 50, Color(0xFFE91E63))
}

data class BirdState(
    val y: Float = 0.45f,          // 0f (top) to 1f (ground)
    val velocity: Float = 0f,      // vertical velocity
    val rotation: Float = 0f,      // degrees (-30f up to +70f down)
    val wingAngle: Float = 0f,     // wing flap cycle
    val isFlapping: Boolean = false
)

data class CactusObstacle(
    val id: Long,
    val x: Float,                  // Screen width fraction (e.g. 1.2f entering right edge)
    val gapCenterY: Float,         // Gap vertical center (e.g. 0.35f to 0.65f)
    val gapHeight: Float,          // Size of opening (e.g. 0.26f)
    val width: Float = 0.125f,     // Width fraction of screen (further camera view)
    val passed: Boolean = false,
    val hasCoin: Boolean = false,
    val coinCollected: Boolean = false,
    val hasLeftArm: Boolean = true,
    val hasRightArm: Boolean = true
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val alpha: Float,
    val color: Color,
    val life: Float,               // remaining life fraction 1.0f down to 0f
    val decay: Float = 0.03f
)

data class GameStats(
    val bestScore: Int = 0,
    val totalGamesPlayed: Int = 0,
    val totalCactiPassed: Int = 0,
    val totalCoinsCollected: Int = 0
)

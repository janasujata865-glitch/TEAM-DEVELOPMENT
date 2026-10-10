package com.example.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudio
import com.example.data.GamePreferences
import com.example.model.BirdSkin
import com.example.model.BirdState
import com.example.model.CactusObstacle
import com.example.model.DesertTimeOfDay
import com.example.model.GameStats
import com.example.model.GameStatus
import com.example.model.MedalType
import com.example.model.Particle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = GamePreferences(application)
    val audio = GameAudio(application)

    // Game states
    private val _gameStatus = MutableStateFlow(GameStatus.READY)
    val gameStatus: StateFlow<GameStatus> = _gameStatus.asStateFlow()

    private val _birdState = MutableStateFlow(BirdState())
    val birdState: StateFlow<BirdState> = _birdState.asStateFlow()

    private val _cacti = MutableStateFlow<List<CactusObstacle>>(emptyList())
    val cacti: StateFlow<List<CactusObstacle>> = _cacti.asStateFlow()

    private val _particles = MutableStateFlow<List<Particle>>(emptyList())
    val particles: StateFlow<List<Particle>> = _particles.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _coinsCollectedInGame = MutableStateFlow(0)
    val coinsCollectedInGame: StateFlow<Int> = _coinsCollectedInGame.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    // Preferences mirrors
    val stats: StateFlow<GameStats> = preferences.stats
    val selectedSkin: StateFlow<BirdSkin> = preferences.selectedSkin
    val timeOfDay: StateFlow<DesertTimeOfDay> = preferences.timeOfDay
    val isSoundEnabled: StateFlow<Boolean> = preferences.isSoundEnabled
    val isVibrateEnabled: StateFlow<Boolean> = preferences.isVibrateEnabled

    // Parallax scroll offsets
    private val _groundScroll = MutableStateFlow(0f)
    val groundScroll: StateFlow<Float> = _groundScroll.asStateFlow()

    private val _dunesScroll = MutableStateFlow(0f)
    val dunesScroll: StateFlow<Float> = _dunesScroll.asStateFlow()

    private val _cloudsScroll = MutableStateFlow(0f)
    val cloudsScroll: StateFlow<Float> = _cloudsScroll.asStateFlow()

    // Progressive speed and flight timer states
    private var flightTimeMs = 0L
    private var lastAnnouncedLevel = 0

    private val _flightDurationSeconds = MutableStateFlow(0)
    val flightDurationSeconds: StateFlow<Int> = _flightDurationSeconds.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0f)
    val speedMultiplier: StateFlow<Float> = _speedMultiplier.asStateFlow()

    private val _speedAnnouncement = MutableStateFlow<String?>(null)
    val speedAnnouncement: StateFlow<String?> = _speedAnnouncement.asStateFlow()

    // Phonk audio synchronization: screen shake and 25% blur pulse
    private val _shakeOffset = MutableStateFlow(Offset.Zero)
    val shakeOffset: StateFlow<Offset> = _shakeOffset.asStateFlow()

    private val _blurRadius = MutableStateFlow(0f)
    val blurRadius: StateFlow<Float> = _blurRadius.asStateFlow()

    private val _phonkDropActive = MutableStateFlow(false)
    val phonkDropActive: StateFlow<Boolean> = _phonkDropActive.asStateFlow()

    private val _bassPulseIntensity = MutableStateFlow(0f)
    val bassPulseIntensity: StateFlow<Float> = _bassPulseIntensity.asStateFlow()

    private var lastTriggeredKickMs = -1

    companion object {
        private const val SONG_DURATION_MS = 94040
        // Exact 808 Bass Kick drop timestamps in milliseconds from Airtel Phonk (phonk_music.mp3)
        private val BASS_KICK_TIMESTAMPS_MS = intArrayOf(
            12161, 12519, 13329, 13693, 14111, 14555, 14841, 15341, 15754, 16144, 16521, 16997,
            17399, 17811, 18264, 18563, 18993, 19331, 19686, 20111, 20396, 20679, 21088, 21740,
            22122, 22511, 23251, 23664, 23986, 24282, 26052, 26555, 26850, 27615, 28296, 28743,
            29236, 29520, 29810, 30128, 30696, 31170, 32408, 32955, 33771, 34964, 35306, 35776,
            36169, 36669, 37043, 37332, 37734, 38277, 38574, 39472, 39961, 40338, 40785, 41124,
            41440, 42536, 42915, 43439, 44096, 44400, 44696, 45080, 45519, 46092, 46437, 46754,
            47158, 47747, 48178, 48483, 48774, 49172, 49820, 50300, 50729, 51067, 51485, 51833,
            52207, 52770, 53097, 53541, 53875, 54396, 54869, 55397, 55891, 56341, 56851, 57213,
            57596, 57953, 58492, 58867, 59199, 59492, 59887, 60265, 60908, 61254, 61673, 62035,
            62326, 62948, 63282, 63573, 63945, 64300, 64602, 65015, 65370, 66919, 67451, 67922,
            68229, 68762, 69083, 69455, 70061, 70387, 70983, 71631, 72523, 73464, 73770, 75331,
            75707, 76549, 77212, 77537, 77865, 78667, 79275, 79637, 79934, 80837, 81427, 81839,
            82334, 82678, 83362, 83898, 84234, 84517, 84919, 85403, 85686, 86037, 86416, 87058,
            87444, 87774, 88069, 88434, 89000, 89569, 89866
        )
    }

    private var gameLoopJob: Job? = null
    private var nextCactusId = 1L
    private var idleTime = 0f

    // Camera perspective: further distance (smaller bird, slimmer cacti, wider desert view)
    val birdX = 0.24f
    val birdRadius = 0.022f
    val groundY = 0.86f

    private val gravity = 0.00062f
    private val flapImpulse = -0.0118f
    private val maxFallVelocity = 0.015f
    private val baseCactusSpeed = 0.0036f
    private val cactusInterval = 0.58f

    init {
        audio.isSoundEnabled = preferences.isSoundEnabled.value
        audio.isVibrateEnabled = preferences.isVibrateEnabled.value
        resetToReady()
        startGameLoop()
    }

    private fun getCurrentSpeed(): Float {
        val speedLevel = (flightTimeMs / 10000L).toInt()
        val addedSpeed = (speedLevel * 0.00018f).coerceAtMost(0.0022f)
        return baseCactusSpeed + addedSpeed
    }

    private fun resetToReady() {
        _birdState.value = BirdState(
            y = 0.45f,
            velocity = 0f,
            rotation = 0f,
            wingAngle = 0f
        )
        _cacti.value = emptyList()
        _particles.value = emptyList()
        _score.value = 0
        _coinsCollectedInGame.value = 0
        _isNewHighScore.value = false
        idleTime = 0f
        flightTimeMs = 0L
        lastAnnouncedLevel = 0
        _flightDurationSeconds.value = 0
        _speedMultiplier.value = 1.0f
        _speedAnnouncement.value = null
        _shakeOffset.value = Offset.Zero
        _blurRadius.value = 0f
        _bassPulseIntensity.value = 0f
        _phonkDropActive.value = false
        lastTriggeredKickMs = -1
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val deltaMs = (now - lastTime) / 1_000_000L
                lastTime = now

                updateGameTick(deltaMs)
                delay(16) // Target ~60 FPS
            }
        }
    }

    private fun updateGameTick(deltaMs: Long) {
        when (_gameStatus.value) {
            GameStatus.READY -> {
                // Gentle floating bobbing animation
                idleTime += 0.06f
                val bobOffset = sin(idleTime) * 0.030f
                val wingCycle = sin(idleTime * 2f) * 20f
                _birdState.value = _birdState.value.copy(
                    y = 0.44f + bobOffset,
                    velocity = 0f,
                    rotation = sin(idleTime) * 8f,
                    wingAngle = wingCycle
                )
                // Gentle background movement
                _groundScroll.value = (_groundScroll.value + 0.0015f) % 1f
                _dunesScroll.value = (_dunesScroll.value + 0.0007f) % 1f
                _cloudsScroll.value = (_cloudsScroll.value + 0.00025f) % 1f
                updateParticles()
            }
            GameStatus.PLAYING -> {
                // Track flight time and speed progression every 10 seconds
                flightTimeMs += deltaMs
                val currentSeconds = (flightTimeMs / 1000L).toInt()
                _flightDurationSeconds.value = currentSeconds

                val speedLevel = (flightTimeMs / 10000L).toInt()
                val speedMult = 1.0f + (speedLevel * 0.05f)
                _speedMultiplier.value = speedMult

                // Speed increase announcement every 10s
                if (speedLevel > lastAnnouncedLevel && speedLevel > 0) {
                    lastAnnouncedLevel = speedLevel
                    val banner = "⚡ SPEED +5% (${String.format("%.2f", speedMult)}x) ⚡"
                    _speedAnnouncement.value = banner
                    audio.playScore()
                    viewModelScope.launch {
                        delay(2000)
                        if (_speedAnnouncement.value == banner) {
                            _speedAnnouncement.value = null
                        }
                    }
                }

                // Subtle bass slowdown: ekdam thora sa slow (15% slow-mo) during 808 bass kick punch
                val bassSlowdown = if (_bassPulseIntensity.value > 0.08f) {
                    0.85f
                } else {
                    1.0f
                }

                val currentSpeed = getCurrentSpeed() * bassSlowdown
                updatePlayingPhysics(bassSlowdown)
                updateObstacles(currentSpeed)
                updateParticles()
                _groundScroll.value = (_groundScroll.value + currentSpeed) % 1f
                _dunesScroll.value = (_dunesScroll.value + currentSpeed * 0.45f) % 1f
                _cloudsScroll.value = (_cloudsScroll.value + currentSpeed * 0.15f) % 1f
            }
            GameStatus.PAUSED -> {
                // Paused - do not advance
            }
            GameStatus.GAME_OVER -> {
                // If bird is still in the air during game over, let it fall to ground
                val bird = _birdState.value
                if (bird.y < groundY - birdRadius) {
                    val newVel = (bird.velocity + gravity * 1.5f).coerceAtMost(maxFallVelocity)
                    val newY = (bird.y + newVel).coerceAtMost(groundY - birdRadius)
                    val newRot = (bird.rotation + 4f).coerceAtMost(90f)
                    _birdState.value = bird.copy(y = newY, velocity = newVel, rotation = newRot)
                }
                updateParticles()
            }
        }

        // 1. Phonk beat synchronization ONLY with the song's real 808 Bass kicks
        val musicPosMs = audio.getPlaybackPositionMs()
        val loopPosMs = (musicPosMs % SONG_DURATION_MS).toInt()
        val isDrop = loopPosMs >= 12100 // First 12.1s is intro melody, Airtel Phonk bass drops at 12.1s
        _phonkDropActive.value = isDrop

        if (audio.isMusicPlaying() && _gameStatus.value == GameStatus.PLAYING && isDrop) {
            // Reset kick tracker when music loops back to start
            if (loopPosMs < 1000) {
                lastTriggeredKickMs = -1
            }

            // Check if current position matches a real 808 bass kick
            for (kick in BASS_KICK_TIMESTAMPS_MS) {
                val diff = abs(loopPosMs - kick)
                if (diff <= 45 && lastTriggeredKickMs != kick) {
                    lastTriggeredKickMs = kick
                    // ONLY on the heavy 808 bass kick: crisp, punchy shake + 25% blur pulse
                    _shakeOffset.value = Offset(
                        (Random.nextFloat() * 10f - 5f),
                        (Random.nextFloat() * 8f - 4f)
                    )
                    _blurRadius.value = 3.5f // 25% subtle blur pulse
                    _bassPulseIntensity.value = 1.0f
                    break
                }
            }
        }

        // Fast responsive decay so shake and blur vanish quickly (~80-100ms)
        _shakeOffset.value = Offset(_shakeOffset.value.x * 0.55f, _shakeOffset.value.y * 0.55f)
        _blurRadius.value = (_blurRadius.value * 0.58f).coerceAtLeast(0f)
        _bassPulseIntensity.value = (_bassPulseIntensity.value * 0.62f).coerceAtLeast(0f)
    }

    private fun updatePlayingPhysics(bassSlowdown: Float = 1.0f) {
        val current = _birdState.value
        val effectiveGravity = gravity * bassSlowdown
        val newVelocity = (current.velocity + effectiveGravity).coerceAtMost(maxFallVelocity * bassSlowdown)
        val newY = current.y + newVelocity * bassSlowdown

        // Dynamic rotation calculation
        val targetRotation = when {
            newVelocity < 0 -> (-26f).coerceAtLeast(current.rotation - 6f)
            else -> (current.rotation + 3.8f).coerceAtMost(70f)
        }

        // Wing flap cycle
        val newWingAngle = if (newVelocity < -0.002f) {
            sin(System.currentTimeMillis() * 0.035f) * 28f
        } else {
            (current.wingAngle * 0.85f)
        }

        _birdState.value = current.copy(
            y = newY,
            velocity = newVelocity,
            rotation = targetRotation,
            wingAngle = newWingAngle
        )

        // Check ceiling & ground collisions
        if (newY - birdRadius <= 0.02f) {
            triggerGameOver()
            return
        }
        if (newY + birdRadius >= groundY) {
            triggerGameOver()
            return
        }

        // Check cactus collisions
        val currentCacti = _cacti.value
        for (cactus in currentCacti) {
            // Horizontal bounds check
            val cactusHalfWidth = cactus.width / 2f
            val cactusLeft = cactus.x - cactusHalfWidth
            val cactusRight = cactus.x + cactusHalfWidth

            val birdLeft = birdX - birdRadius * 0.82f
            val birdRight = birdX + birdRadius * 0.82f
            val birdTop = newY - birdRadius * 0.82f
            val birdBottom = newY + birdRadius * 0.82f

            if (birdRight > cactusLeft && birdLeft < cactusRight) {
                val gapTop = cactus.gapCenterY - cactus.gapHeight / 2f
                val gapBottom = cactus.gapCenterY + cactus.gapHeight / 2f

                // Hit top cactus or bottom cactus
                if (birdTop < gapTop || birdBottom > gapBottom) {
                    triggerGameOver()
                    return
                }
            }

            // Check coin pickup
            if (cactus.hasCoin && !cactus.coinCollected) {
                val coinDist = hypot((birdX - cactus.x).toDouble(), (newY - cactus.gapCenterY).toDouble()).toFloat()
                if (coinDist < (birdRadius + 0.045f)) {
                    // Pick up coin!
                    collectCoin(cactus.id)
                }
            }
        }
    }

    private fun updateObstacles(speed: Float) {
        val currentList = _cacti.value.toMutableList()

        // Move existing cacti left
        val updated = currentList.map { cactus ->
            val newX = cactus.x - speed
            var wasPassed = cactus.passed
            if (!wasPassed && newX + cactus.width / 2f < birdX) {
                wasPassed = true
                onPassCactus()
            }
            cactus.copy(x = newX, passed = wasPassed)
        }.filter { it.x + it.width > -0.2f }

        val resultList = updated.toMutableList()

        // Spawn new cactus when needed
        val rightmostX = resultList.maxOfOrNull { it.x } ?: 0.6f
        if (rightmostX < 1.0f + cactusInterval) {
            val spawnX = maxOf(rightmostX + cactusInterval, 1.25f)
            val gapCenter = Random.nextFloat() * 0.28f + 0.32f // between 0.32 and 0.60
            val hasCoin = Random.nextFloat() < 0.40f

            resultList.add(
                CactusObstacle(
                    id = nextCactusId++,
                    x = spawnX,
                    gapCenterY = gapCenter,
                    gapHeight = 0.26f,
                    width = 0.125f,
                    hasCoin = hasCoin,
                    hasLeftArm = Random.nextBoolean(),
                    hasRightArm = Random.nextBoolean()
                )
            )
        }

        _cacti.value = resultList
    }

    private fun onPassCactus() {
        _score.value += 1
        audio.playScore()
        if (_score.value > preferences.stats.value.bestScore) {
            _isNewHighScore.value = true
        }
        preferences.saveBestScoreLive(_score.value)
    }

    private fun collectCoin(cactusId: Long) {
        val current = _cacti.value.map {
            if (it.id == cactusId) it.copy(coinCollected = true) else it
        }
        _cacti.value = current
        _score.value += 2
        _coinsCollectedInGame.value += 1
        audio.playCoin()
        if (_score.value > preferences.stats.value.bestScore) {
            _isNewHighScore.value = true
        }
        preferences.saveBestScoreLive(_score.value)

        // Spawn coin sparkle particles
        spawnCoinParticles(birdX, _birdState.value.y)
    }

    fun onScreenTap() {
        when (_gameStatus.value) {
            GameStatus.READY -> {
                _gameStatus.value = GameStatus.PLAYING
                // Initial jump
                flap()
                // Initial cactus spawn
                spawnInitialCacti()
            }
            GameStatus.PLAYING -> {
                flap()
            }
            GameStatus.PAUSED -> {
                // Resume game
                _gameStatus.value = GameStatus.PLAYING
            }
            GameStatus.GAME_OVER -> {
                // Keep game over screen until user selects Play Again or Main Menu
            }
        }
    }

    private fun flap() {
        _birdState.value = _birdState.value.copy(
            velocity = flapImpulse,
            rotation = -25f,
            wingAngle = 30f
        )
        audio.playFlap()

        // Spawn feather/dust particles behind the bird
        spawnFlapParticles(birdX - 0.02f, _birdState.value.y + 0.01f)
    }

    private fun spawnInitialCacti() {
        val initialList = mutableListOf<CactusObstacle>()
        var startX = 1.35f
        for (i in 0 until 3) {
            val gapCenter = Random.nextFloat() * 0.26f + 0.34f
            initialList.add(
                CactusObstacle(
                    id = nextCactusId++,
                    x = startX,
                    gapCenterY = gapCenter,
                    gapHeight = 0.26f,
                    width = 0.125f,
                    hasCoin = (i == 1),
                    hasLeftArm = true,
                    hasRightArm = true
                )
            )
            startX += cactusInterval
        }
        _cacti.value = initialList
    }

    private fun triggerGameOver() {
        if (_gameStatus.value == GameStatus.GAME_OVER) return
        _gameStatus.value = GameStatus.GAME_OVER
        audio.playHit()

        // Impact screen shake & 25% blur on crash
        _shakeOffset.value = Offset(
            (Random.nextFloat() * 22f - 11f),
            (Random.nextFloat() * 22f - 11f)
        )
        _blurRadius.value = 8.0f
        _bassPulseIntensity.value = 1.0f

        // Crash sand explosion particles
        spawnCrashParticles(birdX, _birdState.value.y)

        // Save stats
        val finalScore = _score.value
        val cactiPassed = _score.value
        val coins = _coinsCollectedInGame.value
        preferences.recordGameEnded(finalScore, cactiPassed, coins)
        if (finalScore > preferences.stats.value.bestScore) {
            _isNewHighScore.value = true
        }
    }

    fun resumeMusic() {
        audio.resumeMusic()
    }

    fun pauseMusic() {
        audio.pauseMusic()
    }

    fun releaseMusic() {
        audio.releaseMusic()
    }

    override fun onCleared() {
        super.onCleared()
        audio.releaseMusic()
    }

    fun pauseGame() {
        if (_gameStatus.value == GameStatus.PLAYING) {
            _gameStatus.value = GameStatus.PAUSED
        }
    }

    fun resumeGame() {
        if (_gameStatus.value == GameStatus.PAUSED) {
            _gameStatus.value = GameStatus.PLAYING
        }
    }

    fun playAgain() {
        resetToReady()
        _gameStatus.value = GameStatus.PLAYING
        flap()
        spawnInitialCacti()
    }

    fun goToMainMenu() {
        resetToReady()
        _gameStatus.value = GameStatus.READY
    }

    fun restartGame() {
        goToMainMenu()
    }

    fun selectBirdSkin(skin: BirdSkin) {
        preferences.setSelectedSkin(skin)
    }

    fun selectTimeOfDay(time: DesertTimeOfDay) {
        preferences.setTimeOfDay(time)
    }

    fun toggleSound(): Boolean {
        val enabled = preferences.toggleSound()
        audio.isSoundEnabled = enabled
        return enabled
    }

    fun toggleVibrate(): Boolean {
        val enabled = preferences.toggleVibrate()
        audio.isVibrateEnabled = enabled
        return enabled
    }

    fun getMedalForScore(score: Int): MedalType {
        return when {
            score >= MedalType.RUBY.minScore -> MedalType.RUBY
            score >= MedalType.GOLD.minScore -> MedalType.GOLD
            score >= MedalType.SILVER.minScore -> MedalType.SILVER
            score >= MedalType.BRONZE.minScore -> MedalType.BRONZE
            else -> MedalType.NONE
        }
    }

    // Particle system
    private fun spawnFlapParticles(x: Float, y: Float) {
        val newParticles = (0 until 4).map {
            val angle = Random.nextFloat() * PI.toFloat() * 0.6f + PI.toFloat() * 0.7f
            val speed = Random.nextFloat() * 0.003f + 0.001f
            Particle(
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = Random.nextFloat() * 7f + 5f,
                alpha = 0.8f,
                color = selectedSkin.value.secondaryColor,
                life = 1f,
                decay = 0.06f
            )
        }
        _particles.value = _particles.value + newParticles
    }

    private fun spawnCoinParticles(x: Float, y: Float) {
        val newParticles = (0 until 10).map {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 0.005f + 0.002f
            Particle(
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = Random.nextFloat() * 8f + 6f,
                alpha = 1f,
                color = androidx.compose.ui.graphics.Color(0xFFFFD700),
                life = 1f,
                decay = 0.04f
            )
        }
        _particles.value = _particles.value + newParticles
    }

    private fun spawnCrashParticles(x: Float, y: Float) {
        val newParticles = (0 until 18).map {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 0.008f + 0.002f
            Particle(
                x = x,
                y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed,
                size = Random.nextFloat() * 10f + 6f,
                alpha = 1f,
                color = androidx.compose.ui.graphics.Color(0xFFE0A96D),
                life = 1f,
                decay = 0.035f
            )
        }
        _particles.value = _particles.value + newParticles
    }

    private fun updateParticles() {
        val current = _particles.value
        if (current.isEmpty()) return
        val updated = current.mapNotNull { p ->
            val nextLife = p.life - p.decay
            if (nextLife <= 0f) null
            else p.copy(
                x = p.x + p.vx,
                y = p.y + p.vy,
                alpha = nextLife,
                life = nextLife
            )
        }
        _particles.value = updated
    }
}

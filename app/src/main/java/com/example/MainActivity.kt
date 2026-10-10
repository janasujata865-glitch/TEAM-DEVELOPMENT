package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GameStatus
import com.example.ui.DesertGameCanvas
import com.example.ui.GameOverlay
import com.example.ui.theme.DesertBirdTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DesertBirdTheme {
                DesertBirdApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // User requested: "back ane par automatic song chalu ho jai ga ok"
        viewModel.resumeMusic()
    }

    override fun onPause() {
        super.onPause()
        // User requested: "song game minimaiz par nai chalega or koi ads aiga tobi song Ruk mai ga"
        viewModel.pauseGame()
        viewModel.pauseMusic()
    }

    override fun onStop() {
        super.onStop()
        viewModel.pauseMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releaseMusic()
    }
}

@Composable
fun DesertBirdApp(viewModel: GameViewModel) {
    val gameStatus by viewModel.gameStatus.collectAsStateWithLifecycle()
    val birdState by viewModel.birdState.collectAsStateWithLifecycle()
    val cacti by viewModel.cacti.collectAsStateWithLifecycle()
    val particles by viewModel.particles.collectAsStateWithLifecycle()
    val score by viewModel.score.collectAsStateWithLifecycle()
    val coinsInGame by viewModel.coinsCollectedInGame.collectAsStateWithLifecycle()
    val isNewHighScore by viewModel.isNewHighScore.collectAsStateWithLifecycle()

    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedSkin by viewModel.selectedSkin.collectAsStateWithLifecycle()
    val timeOfDay by viewModel.timeOfDay.collectAsStateWithLifecycle()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val isVibrateEnabled by viewModel.isVibrateEnabled.collectAsStateWithLifecycle()

    val groundScroll by viewModel.groundScroll.collectAsStateWithLifecycle()
    val dunesScroll by viewModel.dunesScroll.collectAsStateWithLifecycle()
    val cloudsScroll by viewModel.cloudsScroll.collectAsStateWithLifecycle()

    val flightDurationSeconds by viewModel.flightDurationSeconds.collectAsStateWithLifecycle()
    val speedMultiplier by viewModel.speedMultiplier.collectAsStateWithLifecycle()
    val speedAnnouncement by viewModel.speedAnnouncement.collectAsStateWithLifecycle()

    // Phonk sync states: screen shake & 25% blur pulse
    val shakeOffset by viewModel.shakeOffset.collectAsStateWithLifecycle()
    val blurRadius by viewModel.blurRadius.collectAsStateWithLifecycle()
    val phonkDropActive by viewModel.phonkDropActive.collectAsStateWithLifecycle()
    val bassPulseIntensity by viewModel.bassPulseIntensity.collectAsStateWithLifecycle()

    // Handle back button behavior
    BackHandler(enabled = gameStatus == GameStatus.PLAYING || gameStatus == GameStatus.PAUSED) {
        if (gameStatus == GameStatus.PLAYING) {
            viewModel.pauseGame()
        } else if (gameStatus == GameStatus.PAUSED) {
            viewModel.restartGame()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF2E1C0C)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val canvasModifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = shakeOffset.x
                    translationY = shakeOffset.y
                }
                .then(
                    if (blurRadius > 0.5f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(blurRadius.dp)
                    } else {
                        Modifier
                    }
                )

            // Full bleed interactive desert game canvas
            DesertGameCanvas(
                birdState = birdState,
                birdXFraction = viewModel.birdX,
                birdRadiusFraction = viewModel.birdRadius,
                groundYFraction = viewModel.groundY,
                cacti = cacti,
                particles = particles,
                birdSkin = selectedSkin,
                timeOfDay = timeOfDay,
                groundScroll = groundScroll,
                dunesScroll = dunesScroll,
                cloudsScroll = cloudsScroll,
                bassPulseIntensity = bassPulseIntensity,
                phonkDropActive = phonkDropActive,
                shakeOffset = shakeOffset,
                blurIntensity = blurRadius,
                onTap = { viewModel.onScreenTap() },
                modifier = canvasModifier
            )

            // Safe area insets overlay for HUD, menus, and controls
            GameOverlay(
                gameStatus = gameStatus,
                score = score,
                coinsInGame = coinsInGame,
                isNewHighScore = isNewHighScore,
                stats = stats,
                selectedSkin = selectedSkin,
                timeOfDay = timeOfDay,
                isSoundEnabled = isSoundEnabled,
                isVibrateEnabled = isVibrateEnabled,
                flightDurationSeconds = flightDurationSeconds,
                speedMultiplier = speedMultiplier,
                speedAnnouncement = speedAnnouncement,
                onScreenTap = { viewModel.onScreenTap() },
                onPauseClick = { viewModel.pauseGame() },
                onResumeClick = { viewModel.resumeGame() },
                onPlayAgain = { viewModel.playAgain() },
                onMainMenu = { viewModel.goToMainMenu() },
                onRestartClick = { viewModel.goToMainMenu() },
                onSelectSkin = { viewModel.selectBirdSkin(it) },
                onSelectTimeOfDay = { viewModel.selectTimeOfDay(it) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleVibrate = { viewModel.toggleVibrate() },
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            )
        }
    }
}

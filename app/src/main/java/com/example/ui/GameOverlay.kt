package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BirdSkin
import com.example.model.DesertTimeOfDay
import com.example.model.GameStats
import com.example.model.GameStatus
import com.example.model.MedalType

@Composable
fun GameOverlay(
    gameStatus: GameStatus,
    score: Int,
    coinsInGame: Int,
    isNewHighScore: Boolean,
    stats: GameStats,
    selectedSkin: BirdSkin,
    timeOfDay: DesertTimeOfDay,
    isSoundEnabled: Boolean,
    isVibrateEnabled: Boolean,
    flightDurationSeconds: Int = 0,
    speedMultiplier: Float = 1.0f,
    speedAnnouncement: String? = null,
    onScreenTap: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onRestartClick: () -> Unit = onMainMenu,
    onSelectSkin: (BirdSkin) -> Unit,
    onSelectTimeOfDay: (DesertTimeOfDay) -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSkinDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // TOP CONTROLS & HUD
        when (gameStatus) {
            GameStatus.READY -> {
                ReadyOverlay(
                    stats = stats,
                    selectedSkin = selectedSkin,
                    timeOfDay = timeOfDay,
                    isSoundEnabled = isSoundEnabled,
                    isVibrateEnabled = isVibrateEnabled,
                    onOpenSkins = { showSkinDialog = true },
                    onOpenStats = { showStatsDialog = true },
                    onSelectTimeOfDay = onSelectTimeOfDay,
                    onToggleSound = onToggleSound,
                    onToggleVibrate = onToggleVibrate
                )
            }
            GameStatus.PLAYING -> {
                PlayingHUD(
                    score = score,
                    coinsInGame = coinsInGame,
                    flightDurationSeconds = flightDurationSeconds,
                    speedMultiplier = speedMultiplier,
                    speedAnnouncement = speedAnnouncement,
                    onPauseClick = onPauseClick
                )
            }
            GameStatus.PAUSED -> {
                PauseOverlay(
                    score = score,
                    flightDurationSeconds = flightDurationSeconds,
                    speedMultiplier = speedMultiplier,
                    isSoundEnabled = isSoundEnabled,
                    isVibrateEnabled = isVibrateEnabled,
                    onResumeClick = onResumeClick,
                    onRestartClick = onRestartClick,
                    onToggleSound = onToggleSound,
                    onToggleVibrate = onToggleVibrate
                )
            }
            GameStatus.GAME_OVER -> {
                GameOverOverlay(
                    score = score,
                    bestScore = stats.bestScore,
                    isNewHighScore = isNewHighScore,
                    coinsInGame = coinsInGame,
                    flightDurationSeconds = flightDurationSeconds,
                    speedMultiplier = speedMultiplier,
                    onPlayAgain = onPlayAgain,
                    onMainMenu = onMainMenu,
                    onOpenSkins = { showSkinDialog = true }
                )
            }
        }

        // Modals
        if (showSkinDialog) {
            SkinSelectorDialog(
                currentSkin = selectedSkin,
                onSkinSelected = {
                    onSelectSkin(it)
                    showSkinDialog = false
                },
                onDismiss = { showSkinDialog = false }
            )
        }

        if (showStatsDialog) {
            StatsDialog(
                stats = stats,
                onDismiss = { showStatsDialog = false }
            )
        }
    }
}

// ========================
// READY / START SCREEN
// ========================
@Composable
private fun ReadyOverlay(
    stats: GameStats,
    selectedSkin: BirdSkin,
    timeOfDay: DesertTimeOfDay,
    isSoundEnabled: Boolean,
    isVibrateEnabled: Boolean,
    onOpenSkins: () -> Unit,
    onOpenStats: () -> Unit,
    onSelectTimeOfDay: (DesertTimeOfDay) -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibrate: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Score Tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC2E1C0C),
                modifier = Modifier.border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Best Score",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BEST: ${stats.bestScore}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            // Quick Actions: Sound, Vibrate, Stats, Skins
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xBB2E1C0C), CircleShape)
                        .testTag("sound_toggle")
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "Toggle Sound",
                        tint = if (isSoundEnabled) Color(0xFFFFD54F) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleVibrate,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xBB2E1C0C), CircleShape)
                        .testTag("vibrate_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Toggle Vibration",
                        tint = if (isVibrateEnabled) Color(0xFFFFD54F) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenStats,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xBB2E1C0C), CircleShape)
                        .testTag("stats_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Game Stats",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Title and Mascot Section
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            // Title Card with Desert Gradient
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xDD3E2723),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .border(2.5.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp))
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DESERT BIRD",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFCC80),
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "🌵 Fly Over The Cacti 🌵",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE082)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center Pulsing "TAP TO FLY" Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(pulseScale)
                .testTag("tap_to_fly_prompt")
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xEEBF360C),
                shadowElevation = 10.dp,
                modifier = Modifier.border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(28.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = "Tap",
                        tint = Color(0xFFFFE082),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "TAP SCREEN TO FLY",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Controls: Time of Day Selector & Bird Skin Selector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            // Time of Day Selector
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC2E1C0C),
                modifier = Modifier.border(1.dp, Color(0x66FFD54F), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeOfDayChip(
                        time = DesertTimeOfDay.DAY,
                        icon = Icons.Default.WbSunny,
                        isSelected = timeOfDay == DesertTimeOfDay.DAY,
                        onSelect = { onSelectTimeOfDay(DesertTimeOfDay.DAY) }
                    )
                    TimeOfDayChip(
                        time = DesertTimeOfDay.SUNSET,
                        icon = Icons.Default.WbTwilight,
                        isSelected = timeOfDay == DesertTimeOfDay.SUNSET,
                        onSelect = { onSelectTimeOfDay(DesertTimeOfDay.SUNSET) }
                    )
                    TimeOfDayChip(
                        time = DesertTimeOfDay.NIGHT,
                        icon = Icons.Default.NightsStay,
                        isSelected = timeOfDay == DesertTimeOfDay.NIGHT,
                        onSelect = { onSelectTimeOfDay(DesertTimeOfDay.NIGHT) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Skin Selector Button
            Button(
                onClick = onOpenSkins,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC3E2723)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .border(1.5.dp, Color(0xFFFFB300), RoundedCornerShape(16.dp))
                    .testTag("skin_select_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Bird Skin",
                    tint = selectedSkin.primaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bird: ${selectedSkin.displayName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TimeOfDayChip(
    time: DesertTimeOfDay,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFE65100) else Color.Transparent
    val contentColor = if (isSelected) Color.White else Color(0xFFBCAAA4)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = Modifier
            .clickable { onSelect() }
            .testTag("time_chip_${time.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = time.displayName,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = time.displayName,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

// ========================
// PLAYING HUD
// ========================
@Composable
private fun PlayingHUD(
    score: Int,
    coinsInGame: Int,
    flightDurationSeconds: Int,
    speedMultiplier: Float,
    speedAnnouncement: String?,
    onPauseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left Stats Pill: Coins & Flight Duration
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (coinsInGame > 0) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xBB2E1C0C),
                        modifier = Modifier.border(1.5.dp, Color(0xFFFFD54F), RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "☀️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$coinsInGame",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                // Speed & Time Counter Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xAA2E1C0C),
                    modifier = Modifier.border(1.dp, Color(0x88FFB300), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ ${flightDurationSeconds}s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFE082)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚡ ${String.format("%.2f", speedMultiplier)}x",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (speedMultiplier > 1.0f) Color(0xFFFF7043) else Color.White
                        )
                    }
                }
            }

            // Big Arcade Score Counter
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.shadow(8.dp),
                    letterSpacing = 2.sp
                )
            }

            // Pause Button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xCC2E1C0C), CircleShape)
                    .border(1.5.dp, Color(0xFFFFD54F), CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pause Game",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Speed Announcement Banner (Every 10 seconds!)
        AnimatedVisibility(
            visible = speedAnnouncement != null,
            enter = fadeIn() + scaleIn()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEEBF360C),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = speedAnnouncement ?: "",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ========================
// PAUSE MODAL
// ========================
@Composable
private fun PauseOverlay(
    score: Int,
    flightDurationSeconds: Int,
    speedMultiplier: Float,
    isSoundEnabled: Boolean,
    isVibrateEnabled: Boolean,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleVibrate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE3E2723)),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(0.85f)
                .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp))
                .testTag("pause_card")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Score: $score  •  Time: ${flightDurationSeconds}s",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current Speed: ${String.format("%.2f", speedMultiplier)}x",
                    fontSize = 13.sp,
                    color = Color(0xFFFFCC80),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Resume Button
                Button(
                    onClick = onResumeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Restart Button
                Button(
                    onClick = onRestartClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restart_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Sound & Vibrate toggles in pause menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0x66FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Sound",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onToggleVibrate,
                        modifier = Modifier
                            .size(46.dp)
                            .background(Color(0x66FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Vibrate",
                            tint = if (isVibrateEnabled) Color(0xFFFFD54F) else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ========================
// GAME OVER OVERLAY
// ========================
@Composable
private fun GameOverOverlay(
    score: Int,
    bestScore: Int,
    isNewHighScore: Boolean,
    coinsInGame: Int,
    flightDurationSeconds: Int,
    speedMultiplier: Float,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    onOpenSkins: () -> Unit
) {
    val medal = when {
        score >= MedalType.RUBY.minScore -> MedalType.RUBY
        score >= MedalType.GOLD.minScore -> MedalType.GOLD
        score >= MedalType.SILVER.minScore -> MedalType.SILVER
        score >= MedalType.BRONZE.minScore -> MedalType.BRONZE
        else -> MedalType.NONE
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x77000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth(0.92f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "GAME OVER" Banner
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFB71C1C),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .border(2.dp, Color(0xFFFF8A80), RoundedCornerShape(20.dp))
                    .testTag("game_over_banner")
            ) {
                Text(
                    text = "GAME OVER",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scorecard Board
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFA3E2723)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.5.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp))
                    .testTag("score_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isNewHighScore) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF6F00),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "★ NEW HIGH SCORE! ★",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Score Row & Best Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current Score
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SCORE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFCC80)
                            )
                            Text(
                                text = "$score",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // Best Score
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BEST",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFCC80)
                            )
                            Text(
                                text = "$bestScore",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Flight Time & Top Speed in Game
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⏱️ Time: ${flightDurationSeconds}s",
                            color = Color(0xFFFFCC80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⚡ Speed: ${String.format("%.2f", speedMultiplier)}x",
                            color = Color(0xFFFFCC80),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Medal Section (if awarded)
                    if (medal != MedalType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x662E1C0C),
                            modifier = Modifier
                                .border(1.dp, medal.color, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Medal",
                                    tint = medal.color,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Medal: ${medal.title}",
                                    fontWeight = FontWeight.Bold,
                                    color = medal.color,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (coinsInGame > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "+$coinsInGame Sun Coins collected!",
                            color = Color(0xFFFFE082),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Play Again Primary Button
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(18.dp))
                    .testTag("play_again_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Again",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY AGAIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Menu Button
            Button(
                onClick = onMainMenu,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(2.dp, Color(0xFF81C784), RoundedCornerShape(18.dp))
                    .testTag("main_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Main Menu",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MAIN MENU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch Bird Skin button
            Button(
                onClick = onOpenSkins,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xBB3E2723)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("change_bird_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Change Bird",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Change Red Bird Skin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ========================
// BIRD SKIN SELECTOR MODAL
// ========================
@Composable
private fun SkinSelectorDialog(
    currentSkin: BirdSkin,
    onSkinSelected: (BirdSkin) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C0C)),
            modifier = Modifier
                .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp))
                .padding(8.dp)
                .testTag("skin_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT RED BIRD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                BirdSkin.entries.forEach { skin ->
                    val isSelected = skin == currentSkin
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF4E2C15) else Color(0xFF3B2310),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFFD54F) else Color(0x44FFB300),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSkinSelected(skin) }
                            .testTag("skin_option_${skin.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Color Preview
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(skin.primaryColor, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = skin.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = skin.description,
                                    fontSize = 12.sp,
                                    color = Color(0xFFD7CCC8)
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color(0xFFFFD54F),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ========================
// STATS MODAL
// ========================
@Composable
private fun StatsDialog(
    stats: GameStats,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1C0C)),
            modifier = Modifier
                .border(2.dp, Color(0xFFFFB300), RoundedCornerShape(24.dp))
                .padding(8.dp)
                .testTag("stats_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DESERT STATS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                StatRow(label = "Best Flight Score", value = "${stats.bestScore}")
                StatRow(label = "Total Games Played", value = "${stats.totalGamesPlayed}")
                StatRow(label = "Total Cacti Passed", value = "${stats.totalCactiPassed}")
                StatRow(label = "Sun Coins Found", value = "${stats.totalCoinsCollected}")

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFFD7CCC8), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

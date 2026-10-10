package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.BirdSkin
import com.example.model.BirdState
import com.example.model.CactusObstacle
import com.example.model.DesertTimeOfDay
import com.example.model.Particle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DesertGameCanvas(
    birdState: BirdState,
    birdXFraction: Float,
    birdRadiusFraction: Float,
    groundYFraction: Float,
    cacti: List<CactusObstacle>,
    particles: List<Particle>,
    birdSkin: BirdSkin,
    timeOfDay: DesertTimeOfDay,
    groundScroll: Float,
    dunesScroll: Float,
    cloudsScroll: Float,
    bassPulseIntensity: Float = 0f,
    phonkDropActive: Boolean = false,
    shakeOffset: Offset = Offset.Zero,
    blurIntensity: Float = 0f,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("game_canvas")
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // 1. Draw Desert Sky & Atmosphere
        drawDesertSky(timeOfDay, canvasWidth, canvasHeight)

        // Phonk Bass Beat Pulse Vignette Glow ONLY on heavy 808 Bass kicks
        if (bassPulseIntensity > 0.15f) {
            val pulseAlpha = (bassPulseIntensity * 0.25f).coerceIn(0f, 0.35f)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x00FF1744),
                        Color(0xFFFF1744).copy(alpha = pulseAlpha)
                    ),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f),
                    radius = maxOf(canvasWidth, canvasHeight) * 0.75f
                ),
                size = Size(canvasWidth, canvasHeight)
            )
        }

        // High-energy Phonk Drop Speed Streaks (ONLY during 808 bass kick punch)
        if (phonkDropActive && bassPulseIntensity > 0.2f) {
            drawPhonkSpeedStreaks(
                groundScroll = groundScroll,
                bassPulse = bassPulseIntensity,
                width = canvasWidth,
                height = canvasHeight * groundYFraction
            )
        }

        // 2. Draw Sun / Moon & Stars
        drawCelestialBodies(timeOfDay, canvasWidth, canvasHeight)

        // 3. Draw Desert Clouds
        drawDesertClouds(timeOfDay, cloudsScroll, canvasWidth, canvasHeight)

        // 4. Draw Distant Parallax Pyramids & Mountain Silhouettes
        drawDistantPyramids(timeOfDay, dunesScroll * 0.4f, canvasWidth, canvasHeight, groundYFraction)

        // 5. Draw Rolling Golden Sand Dunes (Midground)
        drawSandDunes(timeOfDay, dunesScroll, canvasWidth, canvasHeight, groundYFraction)

        // 6. Draw Saguaro Cactus Obstacles & Coins
        cacti.forEach { cactus ->
            drawSaguaroCactusPair(
                cactus = cactus,
                groundYFraction = groundYFraction,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight
            )
        }

        // 7. Draw Sandy Foreground Ground & Dunes Line
        drawSandyGround(timeOfDay, groundScroll, canvasWidth, canvasHeight, groundYFraction)

        // Responsive scale calculations: adapt cleanly to any aspect ratio (tall phones, tablets, foldables)
        val referenceUnit = minOf(canvasWidth * 1.8f, canvasHeight)
        val birdPixelX = birdXFraction * canvasWidth
        val birdPixelY = birdState.y * canvasHeight
        val birdPixelRadius = birdRadiusFraction * referenceUnit

        // 25% Blur / Chromatic Aberration Ghosting during Phonk 808 Bass Drops
        if (blurIntensity > 1.8f) {
            val ghostOffset = (blurIntensity * 1.0f).coerceIn(2f, 6f)
            // Cyan ghost
            drawRedBird(
                skin = birdSkin,
                birdState = birdState,
                centerX = birdPixelX + ghostOffset,
                centerY = birdPixelY,
                radius = birdPixelRadius,
                alpha = 0.25f,
                tintColor = Color(0x6600E5FF)
            )
            // Crimson ghost
            drawRedBird(
                skin = birdSkin,
                birdState = birdState,
                centerX = birdPixelX - ghostOffset,
                centerY = birdPixelY,
                radius = birdPixelRadius,
                alpha = 0.25f,
                tintColor = Color(0x66FF1744)
            )
        }

        // Main Bird
        drawRedBird(
            skin = birdSkin,
            birdState = birdState,
            centerX = birdPixelX,
            centerY = birdPixelY,
            radius = birdPixelRadius
        )

        // Bass Shockwave Ring radiating from bird on heavy bass kick
        if (bassPulseIntensity > 0.2f) {
            val ringRadius = birdPixelRadius * (1.6f + (1f - bassPulseIntensity) * 3.0f)
            drawCircle(
                color = Color(0xFFFF5252).copy(alpha = (bassPulseIntensity * 0.6f).coerceIn(0f, 0.7f)),
                radius = ringRadius,
                center = Offset(birdPixelX, birdPixelY),
                style = Stroke(width = 3.0f * bassPulseIntensity)
            )
        }

        // 9. Draw Particle Effects
        particles.forEach { particle ->
            val px = particle.x * canvasWidth
            val py = particle.y * canvasHeight
            drawCircle(
                color = particle.color.copy(alpha = particle.alpha.coerceIn(0f, 1f)),
                radius = particle.size,
                center = Offset(px, py)
            )
        }

        // 25% Blur Wash overlay (Frosted vignette on beat drop)
        if (blurIntensity > 1.5f) {
            val blurWashAlpha = ((blurIntensity / 3.5f) * 0.18f).coerceIn(0f, 0.18f)
            drawRect(
                color = Color(0x22000000).copy(alpha = blurWashAlpha),
                size = Size(canvasWidth, canvasHeight)
            )
        }
    }
}

// ========================
// 1. DESERT SKY
// ========================
private fun DrawScope.drawDesertSky(
    timeOfDay: DesertTimeOfDay,
    width: Float,
    height: Float
) {
    val skyBrush = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF4FC3F7), // Sunny clear blue
                Color(0xFF81D4FA),
                Color(0xFFFFE082), // Golden warm horizon
                Color(0xFFFFCC80)
            ),
            startY = 0f,
            endY = height * 0.85f
        )
        DesertTimeOfDay.SUNSET -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2C1654), // Twilight violet
                Color(0xFF6A1B9A),
                Color(0xFFD84315), // Fiery sunset orange
                Color(0xFFFF8F00),
                Color(0xFFFFD54F)  // Glowing golden sands horizon
            ),
            startY = 0f,
            endY = height * 0.85f
        )
        DesertTimeOfDay.NIGHT -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B132B), // Deep starfield navy
                Color(0xFF1C2541),
                Color(0xFF3A506B),
                Color(0xFF4B4453)  // Moonlit desert horizon
            ),
            startY = 0f,
            endY = height * 0.85f
        )
    }

    drawRect(
        brush = skyBrush,
        size = Size(width, height)
    )
}

// ========================
// 2. CELESTIAL BODIES (SUN / MOON / STARS)
// ========================
private fun DrawScope.drawCelestialBodies(
    timeOfDay: DesertTimeOfDay,
    width: Float,
    height: Float
) {
    when (timeOfDay) {
        DesertTimeOfDay.DAY -> {
            // Radiant desert sun
            val sunCenter = Offset(width * 0.78f, height * 0.16f)
            val sunRadius = width * 0.11f

            // Outer sun heat haze glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x66FFF59D),
                        Color(0x22FFE082),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = sunRadius * 2.3f
                ),
                radius = sunRadius * 2.3f,
                center = sunCenter
            )

            // Sun core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFDE7), Color(0xFFFFEE58), Color(0xFFFDD835)),
                    center = sunCenter,
                    radius = sunRadius
                ),
                radius = sunRadius,
                center = sunCenter
            )
        }
        DesertTimeOfDay.SUNSET -> {
            // Giant setting sun near horizon
            val sunCenter = Offset(width * 0.45f, height * 0.42f)
            val sunRadius = width * 0.16f

            // Sunset glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x88FF7043), Color(0x33FF5722), Color.Transparent),
                    center = sunCenter,
                    radius = sunRadius * 2.2f
                ),
                radius = sunRadius * 2.2f,
                center = sunCenter
            )

            // Sun disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFB74D), Color(0xFFFF5722)),
                    center = sunCenter,
                    radius = sunRadius
                ),
                radius = sunRadius,
                center = sunCenter
            )
        }
        DesertTimeOfDay.NIGHT -> {
            // Crescent moon
            val moonCenter = Offset(width * 0.8f, height * 0.14f)
            val moonRadius = width * 0.08f

            // Moonlight halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x44E0E0E0), Color.Transparent),
                    center = moonCenter,
                    radius = moonRadius * 2f
                ),
                radius = moonRadius * 2f,
                center = moonCenter
            )

            // Crescent moon shape
            drawCircle(
                color = Color(0xFFFFF9C4),
                radius = moonRadius,
                center = moonCenter
            )
            // Cutout circle for crescent
            drawCircle(
                color = Color(0xFF101B34),
                radius = moonRadius * 0.85f,
                center = Offset(moonCenter.x - moonRadius * 0.38f, moonCenter.y - moonRadius * 0.15f)
            )

            // Desert stars
            val starPositions = listOf(
                Offset(width * 0.12f, height * 0.08f),
                Offset(width * 0.28f, height * 0.13f),
                Offset(width * 0.42f, height * 0.06f),
                Offset(width * 0.62f, height * 0.11f),
                Offset(width * 0.70f, height * 0.05f),
                Offset(width * 0.88f, height * 0.22f),
                Offset(width * 0.18f, height * 0.25f),
                Offset(width * 0.52f, height * 0.20f)
            )
            starPositions.forEachIndexed { idx, pos ->
                val starSize = if (idx % 2 == 0) 3.5f else 2.2f
                drawCircle(color = Color(0xFFFFFDE7), radius = starSize, center = pos)
            }
        }
    }
}

// ========================
// 3. DESERT CLOUDS
// ========================
private fun DrawScope.drawDesertClouds(
    timeOfDay: DesertTimeOfDay,
    cloudsScroll: Float,
    width: Float,
    height: Float
) {
    val cloudColor = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xCCFFFFFF)
        DesertTimeOfDay.SUNSET -> Color(0x99FFA07A)
        DesertTimeOfDay.NIGHT -> Color(0x44607D8B)
    }

    val offset1 = ((cloudsScroll * width * 1.5f) % (width * 1.6f)) - (width * 0.3f)
    val offset2 = (((cloudsScroll + 0.5f) * width * 1.3f) % (width * 1.6f)) - (width * 0.3f)

    drawSingleCloud(Offset(offset1, height * 0.18f), width * 0.26f, cloudColor)
    drawSingleCloud(Offset(offset2, height * 0.29f), width * 0.20f, cloudColor)
}

private fun DrawScope.drawSingleCloud(
    center: Offset,
    cloudWidth: Float,
    color: Color
) {
    val r = cloudWidth * 0.32f
    drawCircle(color = color, radius = r, center = center)
    drawCircle(color = color, radius = r * 0.85f, center = Offset(center.x - r * 0.9f, center.y + r * 0.15f))
    drawCircle(color = color, radius = r * 0.75f, center = Offset(center.x + r * 0.9f, center.y + r * 0.2f))
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - r * 1.3f, center.y),
        size = Size(r * 2.6f, r * 0.8f),
        cornerRadius = CornerRadius(r * 0.4f, r * 0.4f)
    )
}

// ========================
// 4. DISTANT PARALLAX PYRAMIDS
// ========================
private fun DrawScope.drawDistantPyramids(
    timeOfDay: DesertTimeOfDay,
    scroll: Float,
    width: Float,
    height: Float,
    groundYFraction: Float
) {
    val baseGroundY = groundYFraction * height
    val pyramidBaseY = baseGroundY * 0.92f

    val pyramidColor = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xFFD7A762)
        DesertTimeOfDay.SUNSET -> Color(0xFFB04A2F)
        DesertTimeOfDay.NIGHT -> Color(0xFF2C3E50)
    }
    val shadowColor = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xFFB88544)
        DesertTimeOfDay.SUNSET -> Color(0xFF7A2A1A)
        DesertTimeOfDay.NIGHT -> Color(0xFF1F2A38)
    }

    val shift = ((scroll * width * 0.6f) % width) - (width * 0.3f)

    // Great Pyramid 1
    val p1Peak = Offset(width * 0.32f + shift, pyramidBaseY - height * 0.16f)
    val p1Left = Offset(width * 0.10f + shift, pyramidBaseY)
    val p1Right = Offset(width * 0.54f + shift, pyramidBaseY)
    val p1Ridge = Offset(width * 0.30f + shift, pyramidBaseY)

    // Pyramid sunny side
    val pathSunny = Path().apply {
        moveTo(p1Peak.x, p1Peak.y)
        lineTo(p1Left.x, p1Left.y)
        lineTo(p1Ridge.x, p1Ridge.y)
        close()
    }
    drawPath(pathSunny, color = pyramidColor)

    // Pyramid shaded side
    val pathShadow = Path().apply {
        moveTo(p1Peak.x, p1Peak.y)
        lineTo(p1Ridge.x, p1Ridge.y)
        lineTo(p1Right.x, p1Right.y)
        close()
    }
    drawPath(pathShadow, color = shadowColor)

    // Distant smaller Pyramid 2
    val p2Peak = Offset(width * 0.76f + shift, pyramidBaseY - height * 0.11f)
    val p2Left = Offset(width * 0.58f + shift, pyramidBaseY)
    val p2Right = Offset(width * 0.94f + shift, pyramidBaseY)
    val p2Ridge = Offset(width * 0.74f + shift, pyramidBaseY)

    val path2Sunny = Path().apply {
        moveTo(p2Peak.x, p2Peak.y)
        lineTo(p2Left.x, p2Left.y)
        lineTo(p2Ridge.x, p2Ridge.y)
        close()
    }
    drawPath(path2Sunny, color = pyramidColor.copy(alpha = 0.85f))

    val path2Shadow = Path().apply {
        moveTo(p2Peak.x, p2Peak.y)
        lineTo(p2Ridge.x, p2Ridge.y)
        lineTo(p2Right.x, p2Right.y)
        close()
    }
    drawPath(path2Shadow, color = shadowColor.copy(alpha = 0.85f))
}

// ========================
// 5. ROLLING GOLDEN SAND DUNES
// ========================
private fun DrawScope.drawSandDunes(
    timeOfDay: DesertTimeOfDay,
    dunesScroll: Float,
    width: Float,
    height: Float,
    groundYFraction: Float
) {
    val baseY = groundYFraction * height
    val duneColor1 = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xFFE8B66D)
        DesertTimeOfDay.SUNSET -> Color(0xFFD66838)
        DesertTimeOfDay.NIGHT -> Color(0xFF384A5C)
    }
    val duneColor2 = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xFFD99E4B)
        DesertTimeOfDay.SUNSET -> Color(0xFFB54922)
        DesertTimeOfDay.NIGHT -> Color(0xFF283848)
    }

    val offset = ((dunesScroll * width) % width)

    // Midground Dune Curve
    val dunePath = Path().apply {
        moveTo(-width + offset, baseY)
        for (i in -1..2) {
            val startX = i * width + offset
            quadraticTo(
                startX + width * 0.35f, baseY - height * 0.12f,
                startX + width * 0.65f, baseY - height * 0.05f
            )
            quadraticTo(
                startX + width * 0.85f, baseY - height * 0.02f,
                startX + width, baseY
            )
        }
        lineTo(width * 2f, height)
        lineTo(-width * 2f, height)
        close()
    }

    drawPath(
        path = dunePath,
        brush = Brush.verticalGradient(
            colors = listOf(duneColor1, duneColor2),
            startY = baseY - height * 0.12f,
            endY = baseY
        )
    )
}

// ========================
// 6. SAGUARO CACTUS OBSTACLES & COINS
// ========================
private fun DrawScope.drawSaguaroCactusPair(
    cactus: CactusObstacle,
    groundYFraction: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val cactusCenterX = cactus.x * canvasWidth
    // Ensure cactus width scales proportionally with screen width & height
    val cactusWidth = minOf(cactus.width * canvasWidth, canvasHeight * 0.10f)
    val gapCenterPixelY = cactus.gapCenterY * canvasHeight
    val gapHalfHeight = (cactus.gapHeight * canvasHeight) / 2f
    val groundPixelY = groundYFraction * canvasHeight

    val cactusTrunkWidth = cactusWidth * 0.58f

    // Top Cactus: Reaches downwards from the sky / rock arch
    val topCactusBottomY = gapCenterPixelY - gapHalfHeight
    drawInvertedTopCactus(
        centerX = cactusCenterX,
        trunkWidth = cactusTrunkWidth,
        bottomY = topCactusBottomY,
        hasArmLeft = cactus.hasLeftArm,
        hasArmRight = cactus.hasRightArm
    )

    // Bottom Cactus: Grows upward out of the desert sand dunes
    val bottomCactusTopY = gapCenterPixelY + gapHalfHeight
    drawUprightBottomCactus(
        centerX = cactusCenterX,
        trunkWidth = cactusTrunkWidth,
        topY = bottomCactusTopY,
        groundY = groundPixelY,
        hasArmLeft = cactus.hasLeftArm,
        hasArmRight = cactus.hasRightArm
    )

    // Golden Sun Scarab Coin (if present and not yet collected)
    if (cactus.hasCoin && !cactus.coinCollected) {
        val coinRadius = minOf(canvasWidth * 0.038f, canvasHeight * 0.022f)
        drawGoldenSunCoin(
            centerX = cactusCenterX,
            centerY = gapCenterPixelY,
            radius = coinRadius
        )
    }
}

// Draw Upright Bottom Saguaro Cactus
private fun DrawScope.drawUprightBottomCactus(
    centerX: Float,
    trunkWidth: Float,
    topY: Float,
    groundY: Float,
    hasArmLeft: Boolean,
    hasArmRight: Boolean
) {
    val cactusGreenLight = Color(0xFF43A047)
    val cactusGreenMain = Color(0xFF2E7D32)
    val cactusGreenDark = Color(0xFF1B5E20)
    val needleColor = Color(0xFF81C784)
    val flowerColor = Color(0xFFE91E63)

    val halfTrunk = trunkWidth / 2f
    val trunkLeft = centerX - halfTrunk
    val trunkHeight = groundY - topY

    // 1. Cactus Arms (Left and Right branches)
    val armThickness = trunkWidth * 0.42f
    if (hasArmLeft && trunkHeight > 70f) {
        val armBaseY = topY + trunkHeight * 0.42f
        val armTopY = topY + trunkHeight * 0.20f
        val armReachX = centerX - halfTrunk - trunkWidth * 0.65f

        val leftArmPath = Path().apply {
            moveTo(trunkLeft, armBaseY)
            lineTo(armReachX + armThickness, armBaseY)
            lineTo(armReachX + armThickness, armTopY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = armReachX,
                    top = armTopY - armThickness * 0.6f,
                    right = armReachX + armThickness,
                    bottom = armTopY + armThickness * 0.4f
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(armReachX, armBaseY + armThickness * 0.85f)
            lineTo(trunkLeft, armBaseY + armThickness * 0.85f)
            close()
        }
        drawPath(leftArmPath, color = cactusGreenMain)
        drawPath(leftArmPath, color = cactusGreenDark, style = Stroke(width = 2.5f))
    }

    if (hasArmRight && trunkHeight > 70f) {
        val armBaseY = topY + trunkHeight * 0.55f
        val armTopY = topY + trunkHeight * 0.32f
        val armReachX = centerX + halfTrunk + trunkWidth * 0.65f

        val rightArmPath = Path().apply {
            moveTo(centerX + halfTrunk, armBaseY)
            lineTo(armReachX - armThickness, armBaseY)
            lineTo(armReachX - armThickness, armTopY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = armReachX - armThickness,
                    top = armTopY - armThickness * 0.6f,
                    right = armReachX,
                    bottom = armTopY + armThickness * 0.4f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(armReachX, armBaseY + armThickness * 0.85f)
            lineTo(centerX + halfTrunk, armBaseY + armThickness * 0.85f)
            close()
        }
        drawPath(rightArmPath, color = cactusGreenMain)
        drawPath(rightArmPath, color = cactusGreenDark, style = Stroke(width = 2.5f))
    }

    // 2. Main Saguaro Trunk with Rounded Dome Cap
    val domeRadius = halfTrunk
    val trunkPath = Path().apply {
        moveTo(trunkLeft, topY + domeRadius)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = trunkLeft,
                top = topY,
                right = centerX + halfTrunk,
                bottom = topY + domeRadius * 2f
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(centerX + halfTrunk, groundY)
        lineTo(trunkLeft, groundY)
        close()
    }

    // Gradient shading across trunk
    drawPath(
        path = trunkPath,
        brush = Brush.horizontalGradient(
            colors = listOf(cactusGreenLight, cactusGreenMain, cactusGreenDark),
            startX = trunkLeft,
            endX = centerX + halfTrunk
        )
    )
    drawPath(path = trunkPath, color = cactusGreenDark, style = Stroke(width = 3.5f))

    // 3. Saguaro Vertical Ribs / Ridges
    val numRibs = 3
    for (i in 1..numRibs) {
        val ribX = trunkLeft + (trunkWidth / (numRibs + 1)) * i
        drawLine(
            color = cactusGreenDark.copy(alpha = 0.7f),
            start = Offset(ribX, topY + domeRadius * 0.8f),
            end = Offset(ribX, groundY),
            strokeWidth = 2.5f
        )
        // Spines / needles along the ribs
        var spineY = topY + domeRadius * 1.5f
        while (spineY < groundY - 20f) {
            drawCircle(color = needleColor, radius = 2.2f, center = Offset(ribX, spineY))
            spineY += 26f
        }
    }

    // 4. Blooming Red Desert Flower on top!
    val flowerCenter = Offset(centerX, topY)
    val petalRadius = halfTrunk * 0.35f
    for (petalAngle in 0 until 5) {
        val rad = (petalAngle * 72.0 * PI / 180.0)
        val px = flowerCenter.x + (cos(rad) * petalRadius * 0.85f).toFloat()
        val py = flowerCenter.y + (sin(rad) * petalRadius * 0.85f).toFloat() - petalRadius * 0.3f
        drawCircle(color = flowerColor, radius = petalRadius * 0.65f, center = Offset(px, py))
    }
    // Flower golden core
    drawCircle(color = Color(0xFFFFD54F), radius = petalRadius * 0.42f, center = Offset(flowerCenter.x, flowerCenter.y - petalRadius * 0.3f))

    // Base Sand Mound around cactus roots
    drawOval(
        color = Color(0xFFD29054),
        topLeft = Offset(centerX - trunkWidth * 0.9f, groundY - 10f),
        size = Size(trunkWidth * 1.8f, 22f)
    )
}

// Draw Inverted Top Saguaro Cactus (Reaching down from Canyon Arch)
private fun DrawScope.drawInvertedTopCactus(
    centerX: Float,
    trunkWidth: Float,
    bottomY: Float,
    hasArmLeft: Boolean,
    hasArmRight: Boolean
) {
    val cactusGreenLight = Color(0xFF43A047)
    val cactusGreenMain = Color(0xFF2E7D32)
    val cactusGreenDark = Color(0xFF1B5E20)
    val needleColor = Color(0xFF81C784)

    val halfTrunk = trunkWidth / 2f
    val trunkLeft = centerX - halfTrunk
    val topY = 0f
    val trunkHeight = bottomY

    // 1. Inverted Arms
    val armThickness = trunkWidth * 0.42f
    if (hasArmLeft && trunkHeight > 70f) {
        val armBaseY = bottomY - trunkHeight * 0.45f
        val armBottomY = bottomY - trunkHeight * 0.22f
        val armReachX = centerX - halfTrunk - trunkWidth * 0.65f

        val leftArmPath = Path().apply {
            moveTo(trunkLeft, armBaseY)
            lineTo(armReachX + armThickness, armBaseY)
            lineTo(armReachX + armThickness, armBottomY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = armReachX,
                    top = armBottomY - armThickness * 0.4f,
                    right = armReachX + armThickness,
                    bottom = armBottomY + armThickness * 0.6f
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(armReachX, armBaseY - armThickness * 0.85f)
            lineTo(trunkLeft, armBaseY - armThickness * 0.85f)
            close()
        }
        drawPath(leftArmPath, color = cactusGreenMain)
        drawPath(leftArmPath, color = cactusGreenDark, style = Stroke(width = 2.5f))
    }

    if (hasArmRight && trunkHeight > 70f) {
        val armBaseY = bottomY - trunkHeight * 0.35f
        val armBottomY = bottomY - trunkHeight * 0.15f
        val armReachX = centerX + halfTrunk + trunkWidth * 0.65f

        val rightArmPath = Path().apply {
            moveTo(centerX + halfTrunk, armBaseY)
            lineTo(armReachX - armThickness, armBaseY)
            lineTo(armReachX - armThickness, armBottomY)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = armReachX - armThickness,
                    top = armBottomY - armThickness * 0.4f,
                    right = armReachX,
                    bottom = armBottomY + armThickness * 0.6f
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            lineTo(armReachX, armBaseY - armThickness * 0.85f)
            lineTo(centerX + halfTrunk, armBaseY - armThickness * 0.85f)
            close()
        }
        drawPath(rightArmPath, color = cactusGreenMain)
        drawPath(rightArmPath, color = cactusGreenDark, style = Stroke(width = 2.5f))
    }

    // 2. Main Trunk with Rounded Bottom Dome
    val domeRadius = halfTrunk
    val trunkPath = Path().apply {
        moveTo(trunkLeft, topY)
        lineTo(centerX + halfTrunk, topY)
        lineTo(centerX + halfTrunk, bottomY - domeRadius)
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                left = trunkLeft,
                top = bottomY - domeRadius * 2f,
                right = centerX + halfTrunk,
                bottom = bottomY
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        close()
    }

    drawPath(
        path = trunkPath,
        brush = Brush.horizontalGradient(
            colors = listOf(cactusGreenLight, cactusGreenMain, cactusGreenDark),
            startX = trunkLeft,
            endX = centerX + halfTrunk
        )
    )
    drawPath(path = trunkPath, color = cactusGreenDark, style = Stroke(width = 3.5f))

    // Vertical Ribs
    val numRibs = 3
    for (i in 1..numRibs) {
        val ribX = trunkLeft + (trunkWidth / (numRibs + 1)) * i
        drawLine(
            color = cactusGreenDark.copy(alpha = 0.7f),
            start = Offset(ribX, topY),
            end = Offset(ribX, bottomY - domeRadius * 0.8f),
            strokeWidth = 2.5f
        )
        var spineY = 24f
        while (spineY < bottomY - domeRadius * 1.5f) {
            drawCircle(color = needleColor, radius = 2.2f, center = Offset(ribX, spineY))
            spineY += 26f
        }
    }

    // Top Desert Sandstone Canyon Cap
    val sandstoneColor = Color(0xFFC0783E)
    drawRect(
        color = sandstoneColor,
        topLeft = Offset(centerX - trunkWidth * 0.8f, 0f),
        size = Size(trunkWidth * 1.6f, 16f)
    )
}

// Draw Golden Sun Coin in Cactus Gap
private fun DrawScope.drawGoldenSunCoin(
    centerX: Float,
    centerY: Float,
    radius: Float
) {
    // Halo glow
    drawCircle(
        color = Color(0x66FFD700),
        radius = radius * 1.45f,
        center = Offset(centerX, centerY)
    )
    // Coin disc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD700), Color(0xFFFFA000)),
            center = Offset(centerX, centerY),
            radius = radius
        ),
        radius = radius,
        center = Offset(centerX, centerY)
    )
    // Coin border rim
    drawCircle(
        color = Color(0xFFFF8F00),
        radius = radius,
        center = Offset(centerX, centerY),
        style = Stroke(width = 3f)
    )
    // Embossed Sun rays inside coin
    for (ray in 0 until 8) {
        val rad = (ray * 45.0 * PI / 180.0)
        val x1 = centerX + (cos(rad) * radius * 0.35f).toFloat()
        val y1 = centerY + (sin(rad) * radius * 0.35f).toFloat()
        val x2 = centerX + (cos(rad) * radius * 0.72f).toFloat()
        val y2 = centerY + (sin(rad) * radius * 0.72f).toFloat()
        drawLine(
            color = Color(0xFFFFF59D),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 2.5f
        )
    }
    drawCircle(
        color = Color(0xFFFFF59D),
        radius = radius * 0.28f,
        center = Offset(centerX, centerY)
    )
}

// ========================
// 7. SANDY GROUND & DUNES LINE
// ========================
private fun DrawScope.drawSandyGround(
    timeOfDay: DesertTimeOfDay,
    groundScroll: Float,
    width: Float,
    height: Float,
    groundYFraction: Float
) {
    val groundTopY = groundYFraction * height
    val sandColorTop = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xFFE2A660)
        DesertTimeOfDay.SUNSET -> Color(0xFFCC6731)
        DesertTimeOfDay.NIGHT -> Color(0xFF324050)
    }
    val sandColorBottom = when (timeOfDay) {
        DesertTimeOfDay.DAY -> Color(0xFFC4863A)
        DesertTimeOfDay.SUNSET -> Color(0xFFA14717)
        DesertTimeOfDay.NIGHT -> Color(0xFF1E2834)
    }

    // Ground block
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(sandColorTop, sandColorBottom),
            startY = groundTopY,
            endY = height
        ),
        topLeft = Offset(0f, groundTopY),
        size = Size(width, height - groundTopY)
    )

    // Sandy Ridge line at the top of the ground
    drawLine(
        color = Color(0xFFFFE0B2).copy(alpha = 0.85f),
        start = Offset(0f, groundTopY),
        end = Offset(width, groundTopY),
        strokeWidth = 4f
    )

    // Scrolling sand pebbles & ripples
    val pebbleSpacing = width * 0.16f
    val offsetPebbles = ((groundScroll * width) % pebbleSpacing)
    var pebbleX = -pebbleSpacing + offsetPebbles
    while (pebbleX < width + pebbleSpacing) {
        // Pebble 1
        drawOval(
            color = Color(0xFF9E652A).copy(alpha = 0.5f),
            topLeft = Offset(pebbleX, groundTopY + 12f),
            size = Size(10f, 6f)
        )
        // Pebble 2
        drawOval(
            color = Color(0xFFFFD54F).copy(alpha = 0.4f),
            topLeft = Offset(pebbleX + pebbleSpacing * 0.55f, groundTopY + 28f),
            size = Size(14f, 7f)
        )
        pebbleX += pebbleSpacing
    }
}

// ========================
// 8. THE RED BIRD CHARACTER
// ========================
private fun DrawScope.drawRedBird(
    skin: BirdSkin,
    birdState: BirdState,
    centerX: Float,
    centerY: Float,
    radius: Float,
    alpha: Float = 1f,
    tintColor: Color? = null
) {
    rotate(
        degrees = birdState.rotation,
        pivot = Offset(centerX, centerY)
    ) {
        val primaryRed = tintColor?.copy(alpha = alpha) ?: skin.primaryColor.copy(alpha = skin.primaryColor.alpha * alpha)
        val darkRuby = tintColor?.copy(alpha = alpha * 0.8f) ?: skin.secondaryColor.copy(alpha = skin.secondaryColor.alpha * alpha)
        val beakGold = tintColor?.copy(alpha = alpha) ?: skin.accentColor.copy(alpha = skin.accentColor.alpha * alpha)

        // 1. Tail Feathers (pointing back-left)
        val tailPath = Path().apply {
            moveTo(centerX - radius * 0.7f, centerY)
            lineTo(centerX - radius * 1.55f, centerY - radius * 0.55f)
            lineTo(centerX - radius * 1.35f, centerY - radius * 0.05f)
            lineTo(centerX - radius * 1.65f, centerY + radius * 0.35f)
            lineTo(centerX - radius * 0.65f, centerY + radius * 0.45f)
            close()
        }
        drawPath(tailPath, color = darkRuby)
        drawPath(tailPath, color = primaryRed, style = Stroke(width = 2f))

        // 2. Red Crest Feathers on top of head
        val crestPath = Path().apply {
            moveTo(centerX - radius * 0.2f, centerY - radius * 0.85f)
            quadraticTo(
                centerX - radius * 0.55f, centerY - radius * 1.45f,
                centerX - radius * 0.75f, centerY - radius * 1.35f
            )
            quadraticTo(
                centerX - radius * 0.35f, centerY - radius * 1.1f,
                centerX + radius * 0.1f, centerY - radius * 0.9f
            )
            close()
        }
        drawPath(crestPath, color = primaryRed)

        // 3. Main Round Plump Bird Body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryRed, darkRuby),
                center = Offset(centerX - radius * 0.2f, centerY - radius * 0.2f),
                radius = radius * 1.2f
            ),
            radius = radius,
            center = Offset(centerX, centerY)
        )
        // Dark outline for arcade contrast
        drawCircle(
            color = Color(0xFF7F0000).copy(alpha = alpha),
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 3.5f)
        )

        // 4. Creamy Warm Belly Highlight
        val bellyPath = Path().apply {
            moveTo(centerX - radius * 0.25f, centerY + radius * 0.2f)
            quadraticTo(
                centerX + radius * 0.45f, centerY + radius * 0.3f,
                centerX + radius * 0.65f, centerY + radius * 0.75f
            )
            quadraticTo(
                centerX, centerY + radius * 1.05f,
                centerX - radius * 0.55f, centerY + radius * 0.7f
            )
            close()
        }
        drawPath(bellyPath, color = Color(0xFFFFCDD2).copy(alpha = 0.85f * alpha))

        // 5. Cute Round Cheek Blush
        drawCircle(
            color = Color(0xFFFF8A80).copy(alpha = 0.6f * alpha),
            radius = radius * 0.26f,
            center = Offset(centerX + radius * 0.32f, centerY + radius * 0.22f)
        )

        // 6. Beak (Golden Yellow with open beak slit)
        val beakPath = Path().apply {
            moveTo(centerX + radius * 0.72f, centerY - radius * 0.15f)
            lineTo(centerX + radius * 1.55f, centerY + radius * 0.15f)
            lineTo(centerX + radius * 0.65f, centerY + radius * 0.48f)
            close()
        }
        drawPath(beakPath, color = beakGold)
        drawPath(beakPath, color = Color(0xFFE65100).copy(alpha = alpha), style = Stroke(width = 2.8f))
        // Beak inner mouth line
        drawLine(
            color = Color(0xFFBF360C).copy(alpha = alpha),
            start = Offset(centerX + radius * 0.75f, centerY + radius * 0.16f),
            end = Offset(centerX + radius * 1.35f, centerY + radius * 0.16f),
            strokeWidth = 2.5f
        )

        // 7. Expressive Big Eye
        val eyeCenter = Offset(centerX + radius * 0.38f, centerY - radius * 0.28f)
        val eyeRadius = radius * 0.38f

        // White eye base
        drawCircle(color = Color.White.copy(alpha = alpha), radius = eyeRadius, center = eyeCenter)
        drawCircle(color = Color(0xFF212121).copy(alpha = alpha), radius = eyeRadius, center = eyeCenter, style = Stroke(width = 2.5f))

        // Pupil (looking forward)
        val pupilCenter = Offset(eyeCenter.x + eyeRadius * 0.22f, eyeCenter.y)
        drawCircle(color = Color(0xFF1A1A1A).copy(alpha = alpha), radius = eyeRadius * 0.58f, center = pupilCenter)

        // Sparkle / Reflection highlights in eye
        drawCircle(color = Color.White.copy(alpha = alpha), radius = eyeRadius * 0.25f, center = Offset(pupilCenter.x - eyeRadius * 0.18f, pupilCenter.y - eyeRadius * 0.18f))
        drawCircle(color = Color.White.copy(alpha = alpha), radius = eyeRadius * 0.12f, center = Offset(pupilCenter.x + eyeRadius * 0.16f, pupilCenter.y + eyeRadius * 0.16f))

        // 8. Animated Flapping Wing
        val wingPivot = Offset(centerX - radius * 0.15f, centerY + radius * 0.05f)
        rotate(
            degrees = birdState.wingAngle,
            pivot = wingPivot
        ) {
            val wingWidth = radius * 1.05f
            val wingHeight = radius * 0.62f
            val wingPath = Path().apply {
                moveTo(wingPivot.x, wingPivot.y)
                quadraticTo(
                    wingPivot.x - wingWidth * 0.8f, wingPivot.y - wingHeight * 1.3f,
                    wingPivot.x - wingWidth * 1.25f, wingPivot.y - wingHeight * 0.2f
                )
                quadraticTo(
                    wingPivot.x - wingWidth * 0.7f, wingPivot.y + wingHeight * 0.7f,
                    wingPivot.x + wingWidth * 0.2f, wingPivot.y + wingHeight * 0.4f
                )
                close()
            }
            drawPath(
                path = wingPath,
                brush = Brush.linearGradient(
                    colors = listOf(primaryRed, darkRuby),
                    start = Offset(wingPivot.x, wingPivot.y),
                    end = Offset(wingPivot.x - wingWidth, wingPivot.y)
                )
            )
            drawPath(path = wingPath, color = Color(0xFF7F0000).copy(alpha = alpha), style = Stroke(width = 2.5f))

            // Wing feather separation lines
            drawLine(
                color = Color(0xFFFFCDD2).copy(alpha = 0.45f * alpha),
                start = Offset(wingPivot.x - wingWidth * 0.3f, wingPivot.y - wingHeight * 0.2f),
                end = Offset(wingPivot.x - wingWidth * 0.85f, wingPivot.y + wingHeight * 0.1f),
                strokeWidth = 2f
            )
        }
    }
}

// ========================
// 9. PHONK DROP DRIFT SPEED STREAKS
// ========================
private fun DrawScope.drawPhonkSpeedStreaks(
    groundScroll: Float,
    bassPulse: Float,
    width: Float,
    height: Float
) {
    val streakCount = 8
    val spacing = width / streakCount
    for (i in 0 until streakCount) {
        val seed = i * 7919
        val y = ((seed % 100) / 100f) * height * 0.75f + height * 0.08f
        val speedFactor = 1.2f + (i % 3) * 0.4f
        val x = ((width * 1.5f - (groundScroll * width * speedFactor * 4f + i * spacing)) % (width * 1.5f))
        val streakLength = width * 0.18f * (1f + bassPulse * 0.8f)
        val alpha = ((0.22f + bassPulse * 0.45f) * (0.6f + (i % 2) * 0.4f)).coerceIn(0f, 0.75f)

        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFF5252).copy(alpha = alpha),
                    Color(0xFFFFD700).copy(alpha = alpha * 0.85f)
                ),
                startX = x - streakLength,
                endX = x
            ),
            start = Offset(x - streakLength, y),
            end = Offset(x, y),
            strokeWidth = 2.5f + (bassPulse * 2.5f)
        )
    }
}

package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DesertColorScheme = darkColorScheme(
    primary = DesertPrimary,
    onPrimary = DesertSandLight,
    secondary = DesertSecondary,
    onSecondary = DesertSandDark,
    tertiary = DesertTertiary,
    background = DesertSandDark,
    onBackground = DesertSandLight,
    surface = DesertSurface,
    onSurface = DesertOnSurface
)

@Composable
fun DesertBirdTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DesertColorScheme.background.toArgb()
                window.navigationBarColor = DesertColorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DesertColorScheme,
        typography = Typography,
        content = content
    )
}

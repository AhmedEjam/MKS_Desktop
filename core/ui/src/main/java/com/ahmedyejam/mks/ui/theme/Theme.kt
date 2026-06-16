package com.ahmedyejam.mks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DawnColorScheme = lightColorScheme(
    primary = DawnPrimary,
    onPrimary = DawnOnPrimary,
    primaryContainer = DawnPrimaryContainer,
    onPrimaryContainer = DawnOnPrimaryContainer,
    secondary = DawnSecondary,
    tertiary = DawnTertiary,
    background = DawnBackground,
    surface = DawnSurface,
    surfaceVariant = DawnSurfaceVariant,
    onBackground = DawnOnBackground,
    onSurface = DawnOnSurface,
    onSurfaceVariant = DawnOnSurfaceVariant,
    outline = DawnOutline,
    error = DawnError,
    errorContainer = DawnErrorContainer,
    onErrorContainer = DawnOnErrorContainer
)

private val ForestColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    tertiary = ForestTertiary,
    background = ForestBackground,
    surface = ForestSurface,
    surfaceVariant = ForestSurfaceVariant,
    onBackground = ForestOnBackground,
    onSurface = ForestOnSurface,
    onSurfaceVariant = ForestOnSurfaceVariant,
    outline = ForestOutline,
    error = ForestError,
    errorContainer = ForestErrorContainer,
    onErrorContainer = ForestOnErrorContainer
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    tertiary = MidnightTertiary,
    background = MidnightBackground,
    surface = MidnightSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onBackground = MidnightOnBackground,
    onSurface = MidnightOnSurface,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    outline = MidnightOutline,
    error = MidnightError,
    errorContainer = MidnightErrorContainer,
    onErrorContainer = MidnightOnErrorContainer
)

private val LavenderColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderPrimaryContainer,
    onPrimaryContainer = LavenderOnPrimaryContainer,
    secondary = LavenderSecondary,
    tertiary = LavenderTertiary,
    background = LavenderBackground,
    surface = LavenderSurface,
    surfaceVariant = LavenderSurfaceVariant,
    onBackground = LavenderOnBackground,
    onSurface = LavenderOnSurface,
    onSurfaceVariant = LavenderOnSurfaceVariant,
    outline = LavenderOutline,
    error = LavenderError,
    errorContainer = LavenderErrorContainer,
    onErrorContainer = LavenderOnErrorContainer
)

fun normalizeMksThemeMode(themeMode: String?): String {
    val mode = themeMode?.uppercase()?.trim() ?: "DAWN"
    return if (mode in setOf("DAWN", "FOREST", "MIDNIGHT", "LAVENDER", "PLAIN_LIGHT", "PLAIN_DARK", "SYSTEM")) mode else "DAWN"
}

@Composable
fun MKSTheme(
    themeMode: String = "DAWN",
    dynamicColor: Boolean = false,
    fontScale: Float = 1.0f,
    uiDensity: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val normalizedTheme = normalizeMksThemeMode(themeMode)

    val colorScheme = when (normalizedTheme) {
        "PLAIN_LIGHT" -> lightColorScheme()
        "PLAIN_DARK" -> darkColorScheme()
        "SYSTEM" -> if (darkTheme) darkColorScheme() else lightColorScheme()
        "FOREST" -> ForestColorScheme
        "MIDNIGHT" -> MidnightColorScheme
        "LAVENDER" -> LavenderColorScheme
        else -> DawnColorScheme
    }

    val isActuallyDark = when (normalizedTheme) {
        "PLAIN_DARK" -> true
        "MIDNIGHT" -> true
        "SYSTEM" -> darkTheme
        else -> false
    }

    val isPlain = normalizedTheme == "PLAIN_LIGHT" || normalizedTheme == "PLAIN_DARK" || normalizedTheme == "SYSTEM"

    val designTokens = MksDesignTokens(
        success = Color(0xFF3C7A54),
        warning = Color(0xFFC47A1C),
        selected = if (isActuallyDark) Color(0xFFF27D52) else Color(0xFFD65A2C),
        correct = Color(0xFF2E7D32),
        wrong = Color(0xFFBA1A1A),
        isPlain = isPlain,
        themeName = normalizedTheme,
        cardRadius = if (isPlain) 12.dp else 18.dp,
        chipRadius = if (isPlain) 8.dp else 14.dp,
        cardElevation = if (isPlain) 0.dp else 2.dp,
        useGradients = !isPlain
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isActuallyDark
            insetsController.isAppearanceLightNavigationBars = !isActuallyDark
        }
    }

    val currentDensity = LocalDensity.current
    val customDensity = Density(
        density = currentDensity.density * uiDensity,
        fontScale = currentDensity.fontScale * fontScale
    )

    CompositionLocalProvider(
        LocalDensity provides customDensity,
        LocalMksDesignTokens provides designTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

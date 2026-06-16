package com.ahmedyejam.mks.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedyejam.mks.core.ui.R
import com.ahmedyejam.mks.ui.theme.LocalMksDesignTokens
import com.ahmedyejam.mks.ui.theme.normalizeMksThemeMode

@Composable
fun WelcomeScreen(
    currentLanguage: String,
    themeMode: String,
    onLanguageChanged: (String) -> Unit,
    onStart: () -> Unit
) {
    var showFeatures by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val tokens = LocalMksDesignTokens.current
    val normalizedTheme = normalizeMksThemeMode(themeMode)
    val isSystemDark = isSystemInDarkTheme()
    val isArabic = currentLanguage == "ar"

    val isActuallyDark = when (normalizedTheme) {
        "MIDNIGHT", "PLAIN_DARK" -> true
        "SYSTEM" -> isSystemDark
        else -> false
    }

    val heroRes = when {
        normalizedTheme == "FOREST" -> R.drawable.welcome_hero_forest
        normalizedTheme == "LAVENDER" || normalizedTheme == "PLAIN_LIGHT" -> R.drawable.welcome_hero_lavender
        isActuallyDark -> R.drawable.welcome_hero_midnight
        else -> R.drawable.welcome_hero_dawn
    }

    val background = when {
        !tokens.useGradients -> Brush.verticalGradient(listOf(colors.background, colors.background))
        isActuallyDark -> {
            Brush.verticalGradient(
                listOf(
                    colors.background,
                    colors.surface.copy(alpha = 0.96f),
                    colors.background
                )
            )
        }
        else -> {
            Brush.verticalGradient(
                listOf(
                    colors.background,
                    colors.primaryContainer.copy(alpha = 0.10f),
                    colors.background
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            WelcomeLanguageToggle(
                currentLanguage = currentLanguage,
                onLanguageChanged = onLanguageChanged,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.welcome_app_title),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (isArabic) 33.sp else 37.sp,
                    letterSpacing = if (isArabic) 0.5.sp else 4.5.sp
                ),
                color = if (isActuallyDark) colors.primary else colors.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.welcome_app_subtitle),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.0.sp),
                color = colors.primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            Image(
                painter = painterResource(id = heroRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(if (isActuallyDark) 0.74f else 0.78f)
                    .aspectRatio(1f),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.welcome_intro_text),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = if (isArabic) 27.sp else 25.sp,
                modifier = Modifier.fillMaxWidth(if (isArabic) 0.90f else 0.82f)
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = if (index == 0) colors.primary else colors.outline.copy(alpha = 0.20f),
                        modifier = Modifier.size(width = if (index == 0) 19.dp else 7.dp, height = 7.dp),
                        content = {}
                    )
                }
            }

            Spacer(Modifier.height(34.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(tokens.chipRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                )
            ) {
                Text(stringResource(R.string.welcome_get_started), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }

            Spacer(Modifier.height(6.dp))

            TextButton(
                onClick = { showFeatures = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(tokens.chipRadius)
            ) {
                Text(stringResource(R.string.welcome_explore_features), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.weight(1f))
        }
    }

    if (showFeatures) {
        AlertDialog(
            onDismissRequest = { showFeatures = false },
            title = { Text(stringResource(R.string.welcome_features_title)) },
            text = { Text(stringResource(R.string.welcome_features_body)) },
            confirmButton = {
                TextButton(onClick = { showFeatures = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun WelcomeLanguageToggle(
    currentLanguage: String,
    onLanguageChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalMksDesignTokens.current
    val toggleShape = RoundedCornerShape(tokens.chipRadius)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .clip(toggleShape)
                .background(colors.surface.copy(alpha = 0.82f))
                .border(1.dp, colors.outline.copy(alpha = 0.18f), toggleShape)
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguagePill(
                text = "EN",
                selected = currentLanguage == "en",
                onClick = { onLanguageChanged("en") }
            )
            LanguagePill(
                text = "العربية",
                selected = currentLanguage == "ar",
                onClick = { onLanguageChanged("ar") }
            )
        }
    }
}

@Composable
private fun LanguagePill(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tokens = LocalMksDesignTokens.current
    val pillShape = RoundedCornerShape(tokens.chipRadius)
    Surface(
        modifier = Modifier
            .height(26.dp)
            .width(if (text == "EN") 42.dp else 66.dp)
            .clip(pillShape)
            .clickable(onClick = onClick),
        shape = pillShape,
        color = if (selected) colors.primary.copy(alpha = 0.16f) else Color.Transparent,
        contentColor = if (selected) colors.onSurface else colors.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, colors.outline.copy(alpha = 0.18f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

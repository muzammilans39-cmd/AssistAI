package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ContrastMode(val displayName: String) {
    HIGH_CONTRAST_LIGHT("Daylight Color Mode"),
    HIGH_CONTRAST_DARK("Dark Color Mode"),
    ULTRA_LOW_VISION("High Contrast: Black & Yellow"),
    BLACK_AND_WHITE("High Contrast: Black & White");

    val isForcedHighContrast: Boolean
        get() = this == ULTRA_LOW_VISION || this == BLACK_AND_WHITE
}

val LocalContrastMode = compositionLocalOf { ContrastMode.HIGH_CONTRAST_LIGHT }

private val LightColorScheme = lightColorScheme(
    primary = DeepNavyPrimary,
    onPrimary = OnDeepNavyPrimary,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = OnNavyPrimaryContainer,
    secondary = WarmGoldSecondary,
    onSecondary = OnWarmGoldSecondary,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = OnGoldSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = OnTealTertiaryContainer,
    background = AccessibleLightBackground,
    onBackground = OnAccessibleLightBackground,
    surface = AccessibleLightSurface,
    onSurface = OnAccessibleLightSurface,
    surfaceVariant = AccessibleLightSurfaceVariant,
    onSurfaceVariant = OnAccessibleLightSurfaceVariant,
    outline = AccessibleOutlineLight,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonLight,
    onErrorContainer = Color(0xFF450A0A)
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueDarkPrimary,
    onPrimary = OnSkyBlueDarkPrimary,
    primaryContainer = DarkNavyContainer,
    onPrimaryContainer = OnDarkNavyContainer,
    secondary = BrightGoldDarkSecondary,
    onSecondary = OnBrightGoldDarkSecondary,
    secondaryContainer = DarkGoldContainer,
    onSecondaryContainer = OnDarkGoldContainer,
    tertiary = MintDarkTertiary,
    onTertiary = OnMintDarkTertiary,
    tertiaryContainer = DarkTealContainer,
    onTertiaryContainer = OnDarkTealContainer,
    background = AccessibleDarkBackground,
    onBackground = OnAccessibleDarkBackground,
    surface = AccessibleDarkSurface,
    onSurface = OnAccessibleDarkSurface,
    surfaceVariant = AccessibleDarkSurfaceVariant,
    onSurfaceVariant = OnAccessibleDarkSurfaceVariant,
    outline = AccessibleOutlineDark
)

private val UltraLowVisionColorScheme = darkColorScheme(
    primary = UltraYellowPrimary,
    onPrimary = UltraPureBlack,
    primaryContainer = Color(0xFF1A1700),
    onPrimaryContainer = UltraYellowPrimary,
    secondary = UltraYellowPrimary,
    onSecondary = UltraPureBlack,
    secondaryContainer = Color(0xFF1F1B00),
    onSecondaryContainer = UltraYellowPrimary,
    tertiary = UltraYellowPrimary,
    onTertiary = UltraPureBlack,
    tertiaryContainer = Color(0xFF1A1700),
    onTertiaryContainer = UltraYellowPrimary,
    background = UltraPureBlack,
    onBackground = UltraYellowPrimary,
    surface = UltraPureBlack,
    onSurface = UltraYellowPrimary,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = UltraYellowPrimary,
    outline = UltraYellowPrimary,
    error = UltraYellowPrimary,
    onError = UltraPureBlack,
    errorContainer = Color(0xFF262100),
    onErrorContainer = UltraYellowPrimary
)

private val BlackAndWhiteColorScheme = darkColorScheme(
    primary = UltraPureWhite,
    onPrimary = UltraPureBlack,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = UltraPureWhite,
    secondary = UltraPureWhite,
    onSecondary = UltraPureBlack,
    secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = UltraPureWhite,
    tertiary = UltraPureWhite,
    onTertiary = UltraPureBlack,
    tertiaryContainer = Color(0xFF1A1A1A),
    onTertiaryContainer = UltraPureWhite,
    background = UltraPureBlack,
    onBackground = UltraPureWhite,
    surface = UltraPureBlack,
    onSurface = UltraPureWhite,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = UltraPureWhite,
    outline = UltraPureWhite,
    error = UltraPureWhite,
    onError = UltraPureBlack,
    errorContainer = Color(0xFF222222),
    onErrorContainer = UltraPureWhite
)

@Composable
fun AssistAITheme(
    contrastMode: ContrastMode = ContrastMode.HIGH_CONTRAST_LIGHT,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = when (contrastMode) {
        ContrastMode.HIGH_CONTRAST_LIGHT -> LightColorScheme
        ContrastMode.HIGH_CONTRAST_DARK -> DarkColorScheme
        ContrastMode.ULTRA_LOW_VISION -> UltraLowVisionColorScheme
        ContrastMode.BLACK_AND_WHITE -> BlackAndWhiteColorScheme
    }

    CompositionLocalProvider(LocalContrastMode provides contrastMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAccessibleTypography(fontScale),
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val mode = if (darkTheme) ContrastMode.HIGH_CONTRAST_DARK else ContrastMode.HIGH_CONTRAST_LIGHT
    AssistAITheme(contrastMode = mode, fontScale = 1.0f, content = content)
}

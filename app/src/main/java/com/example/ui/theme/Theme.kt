package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VaaniExtendedColors(
    val stopAccent: Color,
    val stopContainer: Color,
    val onStopContainer: Color,
    val safeAccent: Color,
    val safeContainer: Color,
    val onSafeContainer: Color,
    val listeningPulse: Color,
    val cardBorder: Color
)

val LocalVaaniExtendedColors = staticCompositionLocalOf {
    VaaniExtendedColors(
        stopAccent = StopCrimson,
        stopContainer = StopCrimsonContainer,
        onStopContainer = OnStopCrimsonContainer,
        safeAccent = ForestTeal,
        safeContainer = ForestTealContainer,
        onSafeContainer = OnForestTealContainer,
        listeningPulse = SaffronBright,
        cardBorder = WarmBorder
    )
}

private val VaaniLightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = DeepIndigo,
    onSecondary = OnDeepIndigo,
    secondaryContainer = DeepIndigoContainer,
    onSecondaryContainer = DeepIndigo,
    tertiary = ForestTeal,
    onTertiary = Color.White,
    tertiaryContainer = ForestTealContainer,
    onTertiaryContainer = OnForestTealContainer,
    error = StopCrimson,
    onError = Color.White,
    errorContainer = StopCrimsonContainer,
    onErrorContainer = OnStopCrimsonContainer,
    background = WarmIvory,
    onBackground = InkPrimary,
    surface = WarmIvory,
    onSurface = InkPrimary,
    surfaceVariant = WarmIvorySurface,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLow = WarmCardWhite,
    surfaceContainer = WarmIvorySurface,
    outline = WarmBorder,
    outlineVariant = WarmBorder
)

private val VaaniHighContrastLightColorScheme = VaaniLightColorScheme.copy(
    primary = HighContrastPrimary,
    secondary = HighContrastSecondary,
    background = HighContrastBg,
    surface = HighContrastBg,
    surfaceVariant = HighContrastSurface,
    onBackground = HighContrastInk,
    onSurface = HighContrastInk,
    onSurfaceVariant = HighContrastInk,
    outline = HighContrastOutline,
    outlineVariant = HighContrastOutline
)

private val VaaniDarkColorScheme = darkColorScheme(
    primary = DarkSaffron,
    onPrimary = DarkOnSaffron,
    primaryContainer = DarkSaffronContainer,
    onPrimaryContainer = Color(0xFFFFE5D4),
    secondary = DarkIndigo,
    onSecondary = DarkOnIndigo,
    secondaryContainer = DarkIndigoContainer,
    onSecondaryContainer = Color(0xFFD9E6F2),
    tertiary = DarkTeal,
    onTertiary = DarkOnTeal,
    tertiaryContainer = DarkTealContainer,
    onTertiaryContainer = Color(0xFFD2F0EA),
    error = DarkCrimson,
    onError = Color(0xFF690005),
    errorContainer = DarkCrimsonContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkCanvas,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkInkMuted,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceHigh,
    outline = Color(0xFF47586B),
    outlineVariant = Color(0xFF334150)
)

@Composable
fun VaaniXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    extraLargeText: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && !darkTheme -> VaaniHighContrastLightColorScheme
        darkTheme -> VaaniDarkColorScheme
        else -> VaaniLightColorScheme
    }

    val extendedColors = if (darkTheme) {
        VaaniExtendedColors(
            stopAccent = DarkCrimson,
            stopContainer = DarkCrimsonContainer,
            onStopContainer = Color(0xFFFFDAD6),
            safeAccent = DarkTeal,
            safeContainer = DarkTealContainer,
            onSafeContainer = Color(0xFFD2F0EA),
            listeningPulse = DarkSaffron,
            cardBorder = Color(0xFF47586B)
        )
    } else {
        VaaniExtendedColors(
            stopAccent = StopCrimson,
            stopContainer = StopCrimsonContainer,
            onStopContainer = OnStopCrimsonContainer,
            safeAccent = ForestTeal,
            safeContainer = ForestTealContainer,
            onSafeContainer = OnForestTealContainer,
            listeningPulse = SaffronBright,
            cardBorder = if (highContrast) HighContrastOutline else WarmBorder
        )
    }

    CompositionLocalProvider(LocalVaaniExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = vaaniTypography(extraLarge = extraLargeText),
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    VaaniXTheme(darkTheme = darkTheme, content = content)
}

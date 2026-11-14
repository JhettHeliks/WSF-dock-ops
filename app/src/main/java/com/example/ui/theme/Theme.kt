package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = WsfMarineTeal,
    onPrimary = PugetDeepNavy,
    primaryContainer = WsfTealContainer,
    onPrimaryContainer = FogWhiteText,
    secondary = RadarCyan,
    onSecondary = PugetDeepNavy,
    secondaryContainer = RadarCyanContainer,
    onSecondaryContainer = FogWhiteText,
    tertiary = BeaconAmberHigh,
    onTertiary = PugetDeepNavy,
    tertiaryContainer = BeaconAmberContainer,
    onTertiaryContainer = BeaconAmberHigh,
    error = SafetyOrangeCritical,
    onError = Color.White,
    errorContainer = SafetyOrangeContainer,
    onErrorContainer = SafetyOrangeCritical,
    background = PugetDeepNavy,
    onBackground = FogWhiteText,
    surface = SteelHullSurface,
    onSurface = FogWhiteText,
    surfaceVariant = DeckPlateElevated,
    onSurfaceVariant = MistSlateSubtext,
    outline = BulkheadBorder
)

private val HighGlareLightColorScheme = lightColorScheme(
    primary = DaylightWsfGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F4EA),
    onPrimaryContainer = DaylightNavyInk,
    secondary = Color(0xFF026AA7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F0FF),
    onSecondaryContainer = DaylightNavyInk,
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = DaylightFoamBg,
    onBackground = DaylightNavyInk,
    surface = DaylightCardSurface,
    onSurface = DaylightNavyInk,
    surfaceVariant = Color(0xFFE2ECEF),
    onSurfaceVariant = Color(0xFF334E58),
    outline = Color(0xFF94B0BA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else HighGlareLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

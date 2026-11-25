package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MontanteSemanticColors(
    val safeEmerald: Color,
    val safeContainer: Color,
    val boostAmber: Color,
    val boostContainer: Color,
    val riskCrimson: Color,
    val riskContainer: Color,
    val telemetryCyan: Color,
    val telemetryContainer: Color,
    val cardElevated: Color,
    val borderSubtle: Color
)

val LocalMontanteColors = staticCompositionLocalOf {
    MontanteSemanticColors(
        safeEmerald = EmeraldPitch,
        safeContainer = EmeraldDeepContainer,
        boostAmber = AmberOdds,
        boostContainer = AmberDeepContainer,
        riskCrimson = CrimsonRisk,
        riskContainer = CrimsonDeepContainer,
        telemetryCyan = CyanTelemetry,
        telemetryContainer = CyanDeepContainer,
        cardElevated = ElevatedCarbon,
        borderSubtle = SlateOutline
    )
}

private val TacticalDarkColorScheme = darkColorScheme(
    primary = EmeraldPitch,
    onPrimary = Color(0xFF022C22),
    primaryContainer = EmeraldDeepContainer,
    onPrimaryContainer = EmeraldSoft,
    secondary = AmberOdds,
    onSecondary = Color(0xFF271300),
    secondaryContainer = AmberDeepContainer,
    onSecondaryContainer = Color(0xFFFCD34D),
    tertiary = CyanTelemetry,
    onTertiary = Color(0xFF04263A),
    tertiaryContainer = CyanDeepContainer,
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = PitchObsidian,
    onBackground = TextPrimaryDark,
    surface = TacticalNavy,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedCarbon,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateOutline,
    outlineVariant = Color(0xFF1E2D3D),
    error = CrimsonRisk,
    onError = Color.White,
    errorContainer = CrimsonDeepContainer,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val TacticalLightColorScheme = lightColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = AmberLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFCBD5E1),
    error = CrimsonRisk,
    onError = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MontanteStatTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TacticalDarkColorScheme else TacticalLightColorScheme
    val semanticColors = if (darkTheme) {
        MontanteSemanticColors(
            safeEmerald = EmeraldPitch,
            safeContainer = EmeraldDeepContainer,
            boostAmber = AmberOdds,
            boostContainer = AmberDeepContainer,
            riskCrimson = CrimsonRisk,
            riskContainer = CrimsonDeepContainer,
            telemetryCyan = CyanTelemetry,
            telemetryContainer = CyanDeepContainer,
            cardElevated = ElevatedCarbon,
            borderSubtle = SlateOutline
        )
    } else {
        MontanteSemanticColors(
            safeEmerald = EmeraldLightPrimary,
            safeContainer = Color(0xFFD1FAE5),
            boostAmber = AmberLightSecondary,
            boostContainer = Color(0xFFFEF3C7),
            riskCrimson = CrimsonRisk,
            riskContainer = Color(0xFFFEE2E2),
            telemetryCyan = Color(0xFF0284C7),
            telemetryContainer = Color(0xFFE0F2FE),
            cardElevated = LightSurface,
            borderSubtle = Color(0xFFCBD5E1)
        )
    }

    CompositionLocalProvider(LocalMontanteColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

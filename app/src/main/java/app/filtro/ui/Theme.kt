package app.filtro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.filtro.core.Tone

private val Light = lightColorScheme(
    primary = Color(0xFF2E5E4E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEAE2),
    onPrimaryContainer = Color(0xFF0E2A20),
    secondary = Color(0xFF59615D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3E8E4),
    onSecondaryContainer = Color(0xFF171D1A),
    tertiary = Color(0xFF8C5A14),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6E3C7),
    onTertiaryContainer = Color(0xFF2D1A00),
    background = Color(0xFFF6F6F3),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF6F6F3),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFE4E7E3),
    onSurfaceVariant = Color(0xFF5B625E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFCFA),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEEFEC),
    surfaceContainerHighest = Color(0xFFE8E9E6),
    outline = Color(0xFFC2C7C3),
    outlineVariant = Color(0xFFE3E6E2),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF8DEDA),
    onErrorContainer = Color(0xFF410E0B),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF93CDB8),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF1D4A3C),
    onPrimaryContainer = Color(0xFFCFEADF),
    secondary = Color(0xFFBEC7C2),
    onSecondary = Color(0xFF29322E),
    secondaryContainer = Color(0xFF2B312E),
    onSecondaryContainer = Color(0xFFDDE4E0),
    tertiary = Color(0xFFE8BE77),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF3D2E16),
    onTertiaryContainer = Color(0xFFF8DFB8),
    background = Color(0xFF111413),
    onBackground = Color(0xFFE2E4E2),
    surface = Color(0xFF111413),
    onSurface = Color(0xFFE2E4E2),
    surfaceVariant = Color(0xFF2A2F2D),
    onSurfaceVariant = Color(0xFFA9B0AC),
    surfaceContainerLowest = Color(0xFF0C0F0E),
    surfaceContainerLow = Color(0xFF161A18),
    surfaceContainer = Color(0xFF1A1E1C),
    surfaceContainerHigh = Color(0xFF222725),
    surfaceContainerHighest = Color(0xFF2B302E),
    outline = Color(0xFF4A524E),
    outlineVariant = Color(0xFF2F3532),
    error = Color(0xFFF2B8B1),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF4A1C17),
    onErrorContainer = Color(0xFFF9DEDA),
)

/** Colores por "tono" de un número (neutral, informativo, precaución, alerta). */
@Immutable
data class ToneColors(val neutral: Color, val info: Color, val caution: Color, val warning: Color) {
    fun of(t: Tone): Color = when (t) {
        Tone.NEUTRAL -> neutral
        Tone.INFO -> info
        Tone.CAUTION -> caution
        Tone.WARNING -> warning
    }
}

private val LightTones = ToneColors(Color(0xFF2E5E4E), Color(0xFF35598C), Color(0xFF9A6312), Color(0xFFB3261E))
private val DarkTones = ToneColors(Color(0xFF93CDB8), Color(0xFFA9C2E6), Color(0xFFE6BC74), Color(0xFFF2A39B))

val LocalTones = staticCompositionLocalOf { LightTones }

private val base = Typography()

private val FiltroType = base.copy(
    displaySmall = base.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 40.sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
    headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

@Composable
fun FiltroTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalTones provides if (dark) DarkTones else LightTones) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = FiltroType, content = content)
    }
}

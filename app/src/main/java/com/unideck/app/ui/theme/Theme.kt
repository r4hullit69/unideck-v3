package com.unideck.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.unideck.app.data.prefs.ThemeMode
import com.unideck.app.data.prefs.UserPreferences

val UniDeckRoundedShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val UniDeckCutShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(7.dp),
    medium = CutCornerShape(11.dp),
    large = CutCornerShape(15.dp),
    extraLarge = CutCornerShape(22.dp)
)

@Suppress("UNUSED_PARAMETER")
fun buildUniDeckShapes(cutCorners: Boolean = false): Shapes = UniDeckRoundedShapes

/**
 * Adjusts primary & tertiary chroma/hue based on Material 3 Palette Style:
 * - TonalSpot (default balanced harmony)
 * - Vibrant (higher saturation + energetic tertiary shift)
 * - Expressive (bold complementary hue rotation)
 */
private fun adjustForPaletteStyle(base: Color, paletteStyle: String, isDark: Boolean): Pair<Color, Color> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(base.toArgb(), hsv)
    val primaryHsv = hsv.clone()
    val tertiaryHsv = hsv.clone()

    when (paletteStyle) {
        "Vibrant" -> {
            primaryHsv[1] = (primaryHsv[1] * 1.18f).coerceIn(0.45f, 0.96f)
            tertiaryHsv[0] = (tertiaryHsv[0] + 54f) % 360f
            tertiaryHsv[1] = (tertiaryHsv[1] * 1.10f).coerceIn(0.42f, 0.92f)
        }
        "Expressive" -> {
            primaryHsv[0] = (primaryHsv[0] + 18f) % 360f
            primaryHsv[1] = (primaryHsv[1] * 1.08f).coerceIn(0.38f, 0.90f)
            tertiaryHsv[0] = (hsv[0] + 115f) % 360f
            tertiaryHsv[1] = (hsv[1] * 0.95f).coerceIn(0.36f, 0.85f)
        }
        else -> { // TonalSpot
            tertiaryHsv[0] = (tertiaryHsv[0] + 42f) % 360f
            tertiaryHsv[1] = (tertiaryHsv[1] * 0.85f).coerceIn(0.28f, 0.72f)
        }
    }
    tertiaryHsv[2] = if (isDark) 0.88f else 0.44f
    return Color(android.graphics.Color.HSVToColor(primaryHsv)) to
        Color(android.graphics.Color.HSVToColor(tertiaryHsv))
}

/**
 * Generates an authentic Google Material You Dark ColorScheme:
 * - When pureTheme == false (Default): Deep seed-tinted charcoal surfaces (tone ~6..17)
 * - When pureTheme == true: True #000000 AMOLED pitch-black canvas with subtle tinted elevation
 */
fun buildDarkScheme(
    palette: UniDeckPalette,
    pureTheme: Boolean,
    paletteStyle: String = "TonalSpot"
): ColorScheme {
    val (primary, tertiary) = adjustForPaletteStyle(palette.darkPrimary, paletteStyle, isDark = true)

    val bg = if (pureTheme) PitchBlackBackground else lerp(Color(0xFF0E1014), primary, 0.065f)
    val surfaceLow = if (pureTheme) PitchBlackCard else lerp(Color(0xFF14171D), primary, 0.095f)
    val surfaceContainer = lerp(surfaceLow, primary, if (pureTheme) 0.045f else 0.065f)
    val surfaceContainerHigh = lerp(surfaceLow, primary, if (pureTheme) 0.085f else 0.115f)
    val surfaceContainerHighest = lerp(surfaceLow, primary, if (pureTheme) 0.13f else 0.165f)

    val primaryContainer = lerp(bg, primary, 0.28f)
    val secondary = lerp(primary, Color(0xFFD8DEE9), 0.36f)
    val secondaryContainer = lerp(surfaceLow, primary, 0.20f)
    val tertiaryContainer = lerp(bg, tertiary, 0.26f)
    val outline = if (pureTheme) PitchBlackBorder else lerp(StandardDarkBorder, primary, 0.14f)

    return darkColorScheme(
        primary = primary,
        onPrimary = palette.onDarkPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = lerp(primary, Color.White, 0.82f),
        secondary = secondary,
        onSecondary = Color(0xFF141821),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = lerp(primary, Color.White, 0.86f),
        tertiary = tertiary,
        onTertiary = Color(0xFF111A1E),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = lerp(tertiary, Color.White, 0.84f),
        background = bg,
        onBackground = Color(0xFFF2F4FA),
        surface = bg,
        onSurface = Color(0xFFF2F4FA),
        surfaceVariant = surfaceContainer,
        onSurfaceVariant = lerp(Color(0xFFA2AABF), primary, 0.12f),
        surfaceContainerLowest = bg,
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.65f),
        error = DangerCoral,
        onError = Color(0xFF2B060D),
        errorContainer = Color(0xFF42111C),
        onErrorContainer = Color(0xFFFFB4C2)
    )
}

/**
 * Generates an authentic Google Material You Light ColorScheme:
 * - Soft seed-tinted canvas (tone ~98) with clean tonal elevation steps
 */
fun buildLightScheme(
    palette: UniDeckPalette,
    pureTheme: Boolean,
    paletteStyle: String = "TonalSpot"
): ColorScheme {
    val (primary, tertiary) = adjustForPaletteStyle(palette.lightPrimary, paletteStyle, isDark = false)

    val bg = if (pureTheme) PureWhiteBackground else lerp(Color(0xFFFAF9FD), primary, 0.032f)
    val surfaceLow = if (pureTheme) PureWhiteCard else lerp(Color(0xFFF3F2F8), primary, 0.048f)
    val surfaceContainer = lerp(surfaceLow, primary, 0.038f)
    val surfaceContainerHigh = lerp(surfaceLow, primary, 0.072f)
    val surfaceContainerHighest = lerp(surfaceLow, primary, 0.11f)

    val primaryContainer = lerp(Color.White, primary, 0.18f)
    val secondary = lerp(primary, Color(0xFF2B3142), 0.35f)
    val secondaryContainer = lerp(Color.White, primary, 0.13f)
    val tertiaryContainer = lerp(Color.White, tertiary, 0.18f)

    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryContainer,
        onPrimaryContainer = lerp(primary, Color.Black, 0.45f),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = lerp(primary, Color.Black, 0.52f),
        tertiary = tertiary,
        onTertiary = Color.White,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = lerp(tertiary, Color.Black, 0.45f),
        background = bg,
        onBackground = Color(0xFF12141D),
        surface = bg,
        onSurface = Color(0xFF12141D),
        surfaceVariant = surfaceContainer,
        onSurfaceVariant = lerp(Color(0xFF535B70), primary, 0.10f),
        surfaceContainerLowest = PureWhiteBackground,
        surfaceContainerLow = surfaceLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = lerp(LightBorder, primary, 0.14f),
        outlineVariant = LightBorder.copy(alpha = 0.7f),
        error = Color(0xFFD62849),
        onError = Color.White,
        errorContainer = Color(0xFFFFE4EA),
        onErrorContainer = Color(0xFF8B0D25)
    )
}

@Composable
fun UniDeckTheme(
    preferences: UserPreferences,
    cutCorners: Boolean = preferences.cutCorners,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (preferences.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val pureTheme = preferences.pitchBlackEnabled
    val pitchBlack = useDark && pureTheme
    val palette = resolveUniDeckPalette(preferences.accentKey)
    val context = LocalContext.current

    val colorScheme = when {
        preferences.dynamicMonetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            when {
                pitchBlack -> base.copy(
                    background = PitchBlackBackground,
                    surface = PitchBlackBackground,
                    surfaceContainerLowest = PitchBlackBackground,
                    surfaceContainerLow = PitchBlackCard,
                    surfaceContainer = lerp(PitchBlackCard, base.primary, 0.055f),
                    surfaceContainerHigh = lerp(PitchBlackCard, base.primary, 0.095f),
                    surfaceContainerHighest = lerp(PitchBlackCard, base.primary, 0.14f)
                )
                !useDark && pureTheme -> base.copy(
                    background = PureWhiteBackground,
                    surface = PureWhiteBackground,
                    surfaceContainerLowest = PureWhiteBackground,
                    surfaceContainerLow = lerp(PureWhiteCard, base.primary, 0.03f)
                )
                else -> base
            }
        }
        useDark -> buildDarkScheme(palette, pureTheme, preferences.paletteStyle)
        else -> buildLightScheme(palette, pureTheme, preferences.paletteStyle)
    }

    val extendedColors = if (useDark) {
        darkExtendedColors(colorScheme.surfaceContainerLow, pitchBlack)
    } else {
        lightExtendedColors(colorScheme.surfaceContainerLow, colorScheme.primary)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            runCatching {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDark
                    isAppearanceLightNavigationBars = !useDark
                }
            }
        }
    }

    CompositionLocalProvider(LocalUniDeckColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildUniDeckTypography(preferences.useSystemFont),
            shapes = buildUniDeckShapes(cutCorners),
            content = content
        )
    }
}
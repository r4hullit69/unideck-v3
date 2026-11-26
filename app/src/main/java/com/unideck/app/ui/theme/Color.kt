package com.unideck.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

data class UniDeckPalette(
    val index: Int,
    val key: String,
    val name: String,
    val swatchColor: Color,
    val darkPrimary: Color,
    val lightPrimary: Color,
    val onDarkPrimary: Color = Color(0xFFFFFFFF)
)

/**
 * 4x4 grid of 16 clean one-word accent palettes.
 */
val UniDeckPalettes: List<UniDeckPalette> = listOf(
    // Row 1
    UniDeckPalette(0, "blue", "Blue", Color(0xFF3B82F6), Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFFFFFFFF)),
    UniDeckPalette(1, "indigo", "Indigo", Color(0xFF6366F1), Color(0xFF6366F1), Color(0xFF4F46E5), Color(0xFFFFFFFF)),
    UniDeckPalette(2, "sky", "Sky", Color(0xFF0EA5E9), Color(0xFF0EA5E9), Color(0xFF0284C7), Color(0xFF031926)),
    UniDeckPalette(3, "cyan", "Cyan", Color(0xFF06B6D4), Color(0xFF06B6D4), Color(0xFF0891B2), Color(0xFF021C22)),
    // Row 2
    UniDeckPalette(4, "teal", "Teal", Color(0xFF14B8A6), Color(0xFF14B8A6), Color(0xFF0D9488), Color(0xFF031F1C)),
    UniDeckPalette(5, "emerald", "Emerald", Color(0xFF10B981), Color(0xFF10B981), Color(0xFF059669), Color(0xFF032016)),
    UniDeckPalette(6, "green", "Green", Color(0xFF22C55E), Color(0xFF22C55E), Color(0xFF16A34A), Color(0xFF042210)),
    UniDeckPalette(7, "lime", "Lime", Color(0xFF84CC16), Color(0xFF84CC16), Color(0xFF65A30D), Color(0xFF172403)),
    // Row 3
    UniDeckPalette(8, "violet", "Violet", Color(0xFF8B5CF6), Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFFFFFFFF)),
    UniDeckPalette(9, "purple", "Purple", Color(0xFFA855F7), Color(0xFFA855F7), Color(0xFF9333EA), Color(0xFFFFFFFF)),
    UniDeckPalette(10, "magenta", "Magenta", Color(0xFFD946EF), Color(0xFFD946EF), Color(0xFFC026D3), Color(0xFF29062E)),
    UniDeckPalette(11, "pink", "Pink", Color(0xFFEC4899), Color(0xFFEC4899), Color(0xFFDB2777), Color(0xFFFFFFFF)),
    // Row 4
    UniDeckPalette(12, "rose", "Rose", Color(0xFFF43F5E), Color(0xFFF43F5E), Color(0xFFE11D48), Color(0xFFFFFFFF)),
    UniDeckPalette(13, "coral", "Coral", Color(0xFFFF6B6B), Color(0xFFFF6B6B), Color(0xFFDC3C3C), Color(0xFF2B0808)),
    UniDeckPalette(14, "orange", "Orange", Color(0xFFF97316), Color(0xFFF97316), Color(0xFFEA580C), Color(0xFF291103)),
    UniDeckPalette(15, "amber", "Amber", Color(0xFFF59E0B), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF261700))
)

/**
 * Resolves a saved accent key gracefully, falling back to Blue if unmatched.
 */
fun resolveUniDeckPalette(accentKey: String?): UniDeckPalette {
    val normalized = accentKey?.trim()?.lowercase()
    return UniDeckPalettes.firstOrNull { it.key == normalized } ?: UniDeckPalettes.first()
}

// Core surface constants
val StandardDarkBackground = Color(0xFF101218)
val StandardDarkCard = Color(0xFF181B24)
val StandardDarkBorder = Color(0xFF262A36)

val PitchBlackBackground = Color(0xFF000000)
val PitchBlackCard = Color(0xFF0D0F14)
val PitchBlackBorder = Color(0xFF1C2029)

val LightBackground = Color(0xFFF4F6FA)
val LightCard = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFDCE1EC)

val PureWhiteBackground = Color(0xFFFFFFFF)
val PureWhiteCard = Color(0xFFF2F4F8)

// Semantic high-chroma status & score colors
val SafeEmerald = Color(0xFF22E58B)
val MarginAmber = Color(0xFFFFB020)
val DangerCoral = Color(0xFFFF4D6A)

val ScoreMidColor = Color(0xFF4D8EFF)
val ScoreQuiz1Color = Color(0xFF22E58B)
val ScoreQuiz2Color = Color(0xFFFFB020)
val ScoreAssignColor = Color(0xFFA67BFF)
val ScoreEndSemColor = Color(0xFF26D9FF)

// Distinct semantic Grade Distribution colors (A+ through E)
val GradeAPlusColor = Color(0xFF22E58B) // Emerald
val GradeAColor = Color(0xFF4D8EFF)     // Blue
val GradeBColor = Color(0xFF26D9FF)     // Cyan
val GradeCColor = Color(0xFFA67BFF)     // Violet
val GradeDColor = Color(0xFFFFB020)     // Amber
val GradeEColor = Color(0xFFFF6B6B)     // Coral

@Immutable
data class UniDeckExtendedColors(
    val isDark: Boolean,
    val isPitchBlack: Boolean,
    val cardSurface: Color,
    val cardBorder: Color,
    val safe: Color,
    val margin: Color,
    val danger: Color,
    val presentFill: Color,
    val presentContent: Color,
    val absentFill: Color,
    val absentContent: Color,
    val cancelledFill: Color,
    val cancelledContent: Color,
    val wordmarkLeading: Color,
    val scoreMid: Color = ScoreMidColor,
    val scoreQuiz1: Color = ScoreQuiz1Color,
    val scoreQuiz2: Color = ScoreQuiz2Color,
    val scoreAssign: Color = ScoreAssignColor,
    val scoreEndSem: Color = ScoreEndSemColor
)

fun darkExtendedColors(
    surfaceContainerLow: Color,
    pitchBlack: Boolean
): UniDeckExtendedColors {
    val cardSurface = if (pitchBlack) PitchBlackCard else surfaceContainerLow
    val border = if (pitchBlack) PitchBlackBorder else StandardDarkBorder
    return UniDeckExtendedColors(
        isDark = true,
        isPitchBlack = pitchBlack,
        cardSurface = cardSurface,
        cardBorder = border,
        safe = SafeEmerald,
        margin = MarginAmber,
        danger = DangerCoral,
        presentFill = Color(0xFF0D2E20),
        presentContent = SafeEmerald,
        absentFill = Color(0xFF33121A),
        absentContent = DangerCoral,
        cancelledFill = Color(0xFF1A1D26),
        cancelledContent = Color(0xFFD0D5E2),
        wordmarkLeading = Color(0xFFFFFFFF)
    )
}

fun lightExtendedColors(
    surfaceContainerLow: Color,
    primary: Color
): UniDeckExtendedColors {
    val tintedCard = lerp(surfaceContainerLow, primary, 0.015f)
    return UniDeckExtendedColors(
        isDark = false,
        isPitchBlack = false,
        cardSurface = tintedCard,
        cardBorder = LightBorder,
        safe = Color(0xFF0A8F52),
        margin = Color(0xFFC47B00),
        danger = Color(0xFFD62849),
        presentFill = Color(0xFFDFF8EB),
        presentContent = Color(0xFF077240),
        absentFill = Color(0xFFFFE4EA),
        absentContent = Color(0xFFC21E3D),
        cancelledFill = Color(0xFFE9ECF3),
        cancelledContent = Color(0xFF475066),
        wordmarkLeading = Color(0xFF10131C)
    )
}

val LocalUniDeckColors = staticCompositionLocalOf {
    darkExtendedColors(StandardDarkCard, pitchBlack = false)
}

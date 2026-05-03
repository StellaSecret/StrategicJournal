package com.stellasecret.strategicjournal.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────
// Color palette — dark, editorial, focused
// ──────────────────────────────────────────────

object JournalColors {
    val Ink = Color(0xFF0D0D0D)
    val InkSoft = Color(0xFF1A1A1A)
    val InkMuted = Color(0xFF2C2C2C)
    val Parchment = Color(0xFFF5F0E8)
    val ParchmentDim = Color(0xFFEDE8E0)
    val Gold = Color(0xFFD4A853)
    val GoldMuted = Color(0xFF8B6E34)
    val Signal = Color(0xFFE05C45) // Accent for decisions / alerts
    val Sage = Color(0xFF6B8F6E) // Hypotheses
    val Slate = Color(0xFF6B7B8D) // Muted text
    val Correct = Color(0xFF4CAF7D)
    val Wrong = Color(0xFFCF6679)
}

private val DarkScheme = darkColorScheme(
    primary = JournalColors.Gold,
    onPrimary = JournalColors.Ink,
    primaryContainer = JournalColors.GoldMuted,
    onPrimaryContainer = JournalColors.Parchment,
    secondary = JournalColors.Sage,
    onSecondary = JournalColors.Ink,
    tertiary = JournalColors.Signal,
    background = JournalColors.Ink,
    onBackground = JournalColors.Parchment,
    surface = JournalColors.InkSoft,
    onSurface = JournalColors.Parchment,
    surfaceVariant = JournalColors.InkMuted,
    onSurfaceVariant = JournalColors.Slate,
    outline = JournalColors.InkMuted,
    error = JournalColors.Wrong,
    onError = JournalColors.Parchment
)

private val LightScheme = lightColorScheme(
    primary = JournalColors.GoldMuted,
    onPrimary = JournalColors.Parchment,
    background = JournalColors.Parchment,
    onBackground = JournalColors.Ink,
    surface = JournalColors.ParchmentDim,
    onSurface = JournalColors.Ink,
    onSurfaceVariant = JournalColors.Slate,
    secondary = JournalColors.Sage,
    tertiary = JournalColors.Signal
)

// ──────────────────────────────────────────────
// Typography — editorial + monospaced feel
// ──────────────────────────────────────────────

// Using system fonts here; in a real project add custom fonts via res/font
val JournalTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)

// ──────────────────────────────────────────────
// Theme
// ──────────────────────────────────────────────

@Composable
fun StrategicJournalTheme(
    darkTheme: Boolean = true, // Default dark — focused, distraction-free
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = JournalTypography,
        content = content
    )
}

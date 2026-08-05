package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

data class NotesThemeConfig(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgCard: Color,
    val textPrimary: Color,
    val textOnCard: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val border: Color,
    val buttonBg: Color,
    val fontFamily: FontFamily
)

object NotesTheme {

    fun getThemeConfig(themeKey: String): NotesThemeConfig {
        return when (themeKey) {
            "polished" -> NotesThemeConfig(
                bgPrimary = Color(0xFFFDFBFF),
                bgSecondary = Color(0xFFEEF1FB),
                bgCard = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF1B1B1F),
                textOnCard = Color(0xFF1B1B1F),
                textSecondary = Color(0xFF44474E),
                textMuted = Color(0xFF74777F),
                accent = Color(0xFF005CBB),
                border = Color(0xFFC4C6D0),
                buttonBg = Color(0xFFD3E2FF),
                fontFamily = FontFamily.SansSerif
            )
            "classified" -> NotesThemeConfig(
                bgPrimary = Color(0xFF1C1E22),
                bgSecondary = Color(0xFF2A2D34),
                bgCard = Color(0xFF363A42),
                textPrimary = Color(0xFFF0F0F0),
                textOnCard = Color(0xFFF0F0F0),
                textSecondary = Color(0xFFFF6B35), // Hazard orange
                textMuted = Color(0x99F0F0F0),
                accent = Color(0xFFF7B801), // Warning yellow
                border = Color(0xFFFF6B35).copy(alpha = 0.3f),
                buttonBg = Color(0xFFFF6B35).copy(alpha = 0.15f),
                fontFamily = FontFamily.Monospace
            )
            "detective" -> NotesThemeConfig(
                bgPrimary = Color(0xFF1A1A1A), // inkwell dark
                bgSecondary = Color(0xFF2B2B2B),
                bgCard = Color(0xFFF4E4C1),  // parchment cream paper
                textPrimary = Color(0xFFEAEAEA),
                textOnCard = Color(0xFF1A1A1A), // dark ink on card paper
                textSecondary = Color(0xFFC41E3A), // crimson stamp
                textMuted = Color(0x99F4E4C1),
                accent = Color(0xFFC41E3A),
                border = Color(0xFF8B0000).copy(alpha = 0.3f),
                buttonBg = Color(0xFFC41E3A).copy(alpha = 0.2f),
                fontFamily = FontFamily.Monospace // classic typewriter feel
            )
            "keep-dark" -> NotesThemeConfig(
                bgPrimary = Color(0xFF202124),
                bgSecondary = Color(0xFF202124),
                bgCard = Color(0xFF2D2E30),
                textPrimary = Color(0xFFE8EAED),
                textOnCard = Color(0xFFE8EAED),
                textSecondary = Color(0xFF9AA0A6),
                textMuted = Color(0x99E8EAED),
                accent = Color(0xFF8AB4F8),
                border = Color(0xFF5F6368),
                buttonBg = Color(0x1F8AB4F8),
                fontFamily = FontFamily.SansSerif
            )
            "keep" -> NotesThemeConfig(
                bgPrimary = Color(0xFFF1F3F4),
                bgSecondary = Color(0xFFFFFFFF),
                bgCard = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF202124),
                textOnCard = Color(0xFF202124),
                textSecondary = Color(0xFF5F6368),
                textMuted = Color(0x99202124),
                accent = Color(0xFF1A73E8),
                border = Color(0x1F000000),
                buttonBg = Color(0x141A73E8),
                fontFamily = FontFamily.SansSerif
            )
            "occult" -> NotesThemeConfig(
                bgPrimary = Color(0xFF0F0B08), // deep charcoal
                bgSecondary = Color(0xFF19120C),
                bgCard = Color(0xFF1A0E09),
                textPrimary = Color(0xFFEADFC8), // ancient parchment gold
                textOnCard = Color(0xFFEADFC8),
                textSecondary = Color(0xFFC9A45F),
                textMuted = Color(0x99EADFC8),
                accent = Color(0xFFD9B56D),
                border = Color(0xFFC9A45F).copy(alpha = 0.25f),
                buttonBg = Color(0xFF5C4422).copy(alpha = 0.3f),
                fontFamily = FontFamily.Serif
            )
            "puppy" -> NotesThemeConfig(
                bgPrimary = Color(0xFF1C4D8E),  // Puppy teal blue
                bgSecondary = Color(0xFFE0DCCB), // tan sidebar
                bgCard = Color(0xFFF0ECE0),      // tan note body
                textPrimary = Color(0xFF1A1A1A),
                textOnCard = Color(0xFF1A1A1A),
                textSecondary = Color(0xFFCC3300), // label crimson
                textMuted = Color(0xFF555555),
                accent = Color(0xFFE8A000),
                border = Color(0xFFA09888),
                buttonBg = Color(0xFFD6D0C0),
                fontFamily = FontFamily.SansSerif
            )
            "terminal" -> NotesThemeConfig(
                bgPrimary = Color(0xFF000000),
                bgSecondary = Color(0xFF0A0A0A),
                bgCard = Color(0xFF111111),
                textPrimary = Color(0xFF00FF41), // monitor green
                textOnCard = Color(0xFF00FF41),
                textSecondary = Color(0xFF33FF33),
                textMuted = Color(0xB300FF41),
                accent = Color(0xFF33FF33),
                border = Color(0xFF33FF33).copy(alpha = 0.5f),
                buttonBg = Color(0xFF33FF33).copy(alpha = 0.15f),
                fontFamily = FontFamily.Monospace
            )
            "xfce" -> NotesThemeConfig(
                bgPrimary = Color(0xFFDCDCDB),  // silver background
                bgSecondary = Color(0xFFEDEDEC),
                bgCard = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF2E3436), // dark charcoal
                textOnCard = Color(0xFF2E3436),
                textSecondary = Color(0xFF4E9A06), // active lime
                textMuted = Color(0x992E3436),
                accent = Color(0xFF3465A4), // GTK blue
                border = Color(0xFFBABDB6),
                buttonBg = Color(0xFFE6E6E5),
                fontFamily = FontFamily.SansSerif
            )
            "roger" -> NotesThemeConfig(
                bgPrimary = Color(0xFFFBF9F4), // warm ivory list background
                bgSecondary = Color(0xFFEFE8DA), // warm sandy card wrapper
                bgCard = Color(0xFFFFFFFF), // crisp white sheet
                textPrimary = Color(0xFF2D1A12), // deep chocolate charcoal (high readability)
                textOnCard = Color(0xFF2D1A12),
                textSecondary = Color(0xFF9E3B1E), // signature terracotta red
                textMuted = Color(0x992D1A12),
                accent = Color(0xFFE2B13C), // golden yellow asterisk
                border = Color(0xFFDDD2BE), // elegant sand outline
                buttonBg = Color(0xFF9E3B1E).copy(alpha = 0.12f),
                fontFamily = FontFamily.Serif // classic writer's typeface
            )
            else -> getThemeConfig("polished")
        }
    }
}

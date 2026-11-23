package com.logicminigames.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class AppThemePalette {
    DYNAMIC,
    CLASSIC,
    PAPER,
    OCEAN,
    FOREST,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class BoardFontScale(val multiplier: Float) {
    SMALL(0.88f),
    NORMAL(1.0f),
    LARGE(1.14f),
    EXTRA_LARGE(1.26f),
}

@Immutable
data class BoardColors(
    val boardSurface: Color,
    val boardOuterBorder: Color,
    val boxDivider: Color,
    val cellDivider: Color,
    val cellDefault: Color,
    val cellFixedBg: Color,
    val cellSelected: Color,
    val cellPeerHighlight: Color,
    val cellSameNumberHighlight: Color,
    val cellHintTarget: Color,
    val cellHintRelated: Color,
    val cellErrorBg: Color,
    val cellUnitPulse: Color,
    val textFixed: Color,
    val textUser: Color,
    val textNotes: Color,
    val textError: Color,
    val textHint: Color,
    val confettiColors: List<Color>,
)

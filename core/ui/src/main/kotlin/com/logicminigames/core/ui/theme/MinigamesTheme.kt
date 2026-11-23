package com.logicminigames.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalBoardColors = staticCompositionLocalOf<BoardColors> {
    error("BoardColors not provided; wrap composable inside MinigamesTheme")
}

val LocalBoardFontScale = staticCompositionLocalOf { BoardFontScale.NORMAL }

val LocalColorblindMode = staticCompositionLocalOf { false }

private val ClassicLightScheme = lightColorScheme(
    primary = Color(0xFF28559A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),
    background = Color(0xFFF9FAFC),
    onBackground = Color(0xFF181C22),
    surface = Color(0xFFF9FAFC),
    onSurface = Color(0xFF181C22),
    surfaceVariant = Color(0xFFE1E5EE),
    onSurfaceVariant = Color(0xFF434852),
    outline = Color(0xFF737884),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val ClassicDarkScheme = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF002F67),
    primaryContainer = Color(0xFF0B3F7D),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF003733),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = Color(0xFF111419),
    onBackground = Color(0xFFE3E6EC),
    surface = Color(0xFF111419),
    onSurface = Color(0xFFE3E6EC),
    surfaceVariant = Color(0xFF282D36),
    onSurfaceVariant = Color(0xFFC2C7D2),
    outline = Color(0xFF8C919C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val PaperLightScheme = lightColorScheme(
    primary = Color(0xFF6F4E37),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5DFCE),
    onPrimaryContainer = Color(0xFF281609),
    secondary = Color(0xFF6C5D52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFE2D7),
    onSecondaryContainer = Color(0xFF261A11),
    tertiary = Color(0xFF4B634E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3EAD5),
    onTertiaryContainer = Color(0xFF0B200F),
    background = Color(0xFFFBF7F0),
    onBackground = Color(0xFF231D17),
    surface = Color(0xFFFBF7F0),
    onSurface = Color(0xFF231D17),
    surfaceVariant = Color(0xFFEDE4D7),
    onSurfaceVariant = Color(0xFF51463C),
    outline = Color(0xFF847669),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val PaperDarkScheme = darkColorScheme(
    primary = Color(0xFFDFBFA7),
    onPrimary = Color(0xFF3E2716),
    primaryContainer = Color(0xFF563B27),
    onPrimaryContainer = Color(0xFFF5DFCE),
    secondary = Color(0xFFD5C4B6),
    onSecondary = Color(0xFF3A2E25),
    secondaryContainer = Color(0xFF514338),
    onSecondaryContainer = Color(0xFFEFE2D7),
    tertiary = Color(0xFFB5CEB7),
    onTertiary = Color(0xFF203523),
    tertiaryContainer = Color(0xFF354B38),
    onTertiaryContainer = Color(0xFFD3EAD5),
    background = Color(0xFF1A1612),
    onBackground = Color(0xFFECE3D8),
    surface = Color(0xFF1A1612),
    onSurface = Color(0xFFECE3D8),
    surfaceVariant = Color(0xFF302922),
    onSurfaceVariant = Color(0xFFD2C4B6),
    outline = Color(0xFF9B8E81),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val OceanLightScheme = lightColorScheme(
    primary = Color(0xFF00637A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBDE9FA),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF3F6371),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE6F1),
    onSecondaryContainer = Color(0xFF07222D),
    tertiary = Color(0xFF3C5BA9),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDAE2FF),
    onTertiaryContainer = Color(0xFF001848),
    background = Color(0xFFF4FAFC),
    onBackground = Color(0xFF121E22),
    surface = Color(0xFFF4FAFC),
    onSurface = Color(0xFF121E22),
    surfaceVariant = Color(0xFFD9E8EE),
    onSurfaceVariant = Color(0xFF3A494F),
    outline = Color(0xFF697A82),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val OceanDarkScheme = darkColorScheme(
    primary = Color(0xFF7FD4EE),
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004D5F),
    onPrimaryContainer = Color(0xFFBDE9FA),
    secondary = Color(0xFFAECBD7),
    onSecondary = Color(0xFF1A3541),
    secondaryContainer = Color(0xFF2D4B58),
    onSecondaryContainer = Color(0xFFCFE6F1),
    tertiary = Color(0xFFB3C5FF),
    onTertiary = Color(0xFF0B2B72),
    tertiaryContainer = Color(0xFF23428C),
    onTertiaryContainer = Color(0xFFDAE2FF),
    background = Color(0xFF0D171B),
    onBackground = Color(0xFFDAE6EB),
    surface = Color(0xFF0D171B),
    onSurface = Color(0xFFDAE6EB),
    surfaceVariant = Color(0xFF1F2F36),
    onSurfaceVariant = Color(0xFFB6C8CF),
    outline = Color(0xFF7E9199),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val ForestLightScheme = lightColorScheme(
    primary = Color(0xFF2D6A4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC7EED7),
    onPrimaryContainer = Color(0xFF062115),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D9),
    onSecondaryContainer = Color(0xFF0B2016),
    tertiary = Color(0xFF3E6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC2E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF5FAF6),
    onBackground = Color(0xFF161E19),
    surface = Color(0xFFF5FAF6),
    onSurface = Color(0xFF161E19),
    surfaceVariant = Color(0xFFDDE7DF),
    onSurfaceVariant = Color(0xFF3F4943),
    outline = Color(0xFF6E7972),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val ForestDarkScheme = darkColorScheme(
    primary = Color(0xFF95D5B2),
    onPrimary = Color(0xFF083923),
    primaryContainer = Color(0xFF185138),
    onPrimaryContainer = Color(0xFFC7EED7),
    secondary = Color(0xFFB4CCBE),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF364B40),
    onSecondaryContainer = Color(0xFFD0E8D9),
    tertiary = Color(0xFFA6CCE0),
    onTertiary = Color(0xFF093544),
    tertiaryContainer = Color(0xFF254B5B),
    onTertiaryContainer = Color(0xFFC2E8FB),
    background = Color(0xFF101712),
    onBackground = Color(0xFFDEE6E0),
    surface = Color(0xFF101712),
    onSurface = Color(0xFFDEE6E0),
    surfaceVariant = Color(0xFF222C25),
    onSurfaceVariant = Color(0xFFBAC6BE),
    outline = Color(0xFF849188),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
private fun buildBoardColors(
    scheme: ColorScheme,
    palette: AppThemePalette,
    isDark: Boolean,
    colorblindMode: Boolean,
): BoardColors {
    val defaultBoard = if (isDark) {
        BoardColors(
            boardSurface = Color(0xFF171B22),
            boardOuterBorder = scheme.outline,
            boxDivider = scheme.onSurface.copy(alpha = 0.78f),
            cellDivider = scheme.outline.copy(alpha = 0.35f),
            cellDefault = Color(0xFF171B22),
            cellFixedBg = Color(0xFF1E242E),
            cellSelected = scheme.primary.copy(alpha = 0.34f),
            cellPeerHighlight = scheme.surfaceVariant.copy(alpha = 0.65f),
            cellSameNumberHighlight = scheme.primary.copy(alpha = 0.22f),
            cellHintTarget = scheme.tertiaryContainer.copy(alpha = 0.72f),
            cellHintRelated = scheme.tertiary.copy(alpha = 0.18f),
            cellErrorBg = scheme.errorContainer.copy(alpha = 0.75f),
            cellUnitPulse = scheme.tertiary.copy(alpha = 0.36f),
            textFixed = scheme.onSurface,
            textUser = scheme.primary,
            textNotes = scheme.onSurfaceVariant,
            textError = scheme.error,
            textHint = scheme.tertiary,
            confettiColors = listOf(scheme.primary, scheme.tertiary, Color(0xFFF59E0B), Color(0xFFEC4899)),
        )
    } else {
        val paperSurface = if (palette == AppThemePalette.PAPER) Color(0xFFFFFCF6) else Color(0xFFFFFFFF)
        val paperFixed = if (palette == AppThemePalette.PAPER) Color(0xFFF3ECE1) else scheme.surfaceVariant.copy(alpha = 0.48f)
        BoardColors(
            boardSurface = paperSurface,
            boardOuterBorder = scheme.onSurface.copy(alpha = 0.80f),
            boxDivider = scheme.onSurface.copy(alpha = 0.80f),
            cellDivider = scheme.outline.copy(alpha = 0.32f),
            cellDefault = paperSurface,
            cellFixedBg = paperFixed,
            cellSelected = scheme.primaryContainer.copy(alpha = 0.85f),
            cellPeerHighlight = scheme.secondaryContainer.copy(alpha = 0.45f),
            cellSameNumberHighlight = scheme.primary.copy(alpha = 0.17f),
            cellHintTarget = scheme.tertiaryContainer,
            cellHintRelated = scheme.tertiary.copy(alpha = 0.16f),
            cellErrorBg = scheme.errorContainer.copy(alpha = 0.85f),
            cellUnitPulse = scheme.tertiary.copy(alpha = 0.28f),
            textFixed = scheme.onSurface,
            textUser = scheme.primary,
            textNotes = scheme.onSurfaceVariant,
            textError = scheme.error,
            textHint = scheme.tertiary,
            confettiColors = listOf(scheme.primary, scheme.tertiary, Color(0xFFD97706), Color(0xFF7C3AED)),
        )
    }

    if (!colorblindMode) return defaultBoard

    // Okabe-Ito colorblind-safe high-contrast overrides (blue / amber-orange / vermilion)
    return if (isDark) {
        defaultBoard.copy(
            cellSelected = Color(0xFF1C4E80),
            cellPeerHighlight = Color(0xFF233040),
            cellSameNumberHighlight = Color(0xFF005A70),
            cellHintTarget = Color(0xFF7A4E00),
            cellHintRelated = Color(0xFF4A3614),
            cellErrorBg = Color(0xFF7C2D12),
            textUser = Color(0xFF56B4E9),
            textError = Color(0xFFFFB77D),
            textHint = Color(0xFFE69F00),
            confettiColors = listOf(Color(0xFF56B4E9), Color(0xFFE69F00), Color(0xFF009E73), Color(0xFFF0E442)),
        )
    } else {
        defaultBoard.copy(
            cellSelected = Color(0xFFBAE1FF),
            cellPeerHighlight = Color(0xFFE6EFF7),
            cellSameNumberHighlight = Color(0xFFCDEEE4),
            cellHintTarget = Color(0xFFFFE4B5),
            cellHintRelated = Color(0xFFFFF3D6),
            cellErrorBg = Color(0xFFFFDFC4),
            textUser = Color(0xFF005B99),
            textError = Color(0xFFB33B00),
            textHint = Color(0xFF9A5B00),
            confettiColors = listOf(Color(0xFF0072B2), Color(0xFFE69F00), Color(0xFF009E73), Color(0xFFD55E00)),
        )
    }
}

@Composable
fun MinigamesTheme(
    palette: AppThemePalette = AppThemePalette.CLASSIC,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorblindMode: Boolean = false,
    fontScale: BoardFontScale = BoardFontScale.NORMAL,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when (palette) {
        AppThemePalette.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) ClassicDarkScheme else ClassicLightScheme
            }
        }
        AppThemePalette.CLASSIC -> if (isDark) ClassicDarkScheme else ClassicLightScheme
        AppThemePalette.PAPER -> if (isDark) PaperDarkScheme else PaperLightScheme
        AppThemePalette.OCEAN -> if (isDark) OceanDarkScheme else OceanLightScheme
        AppThemePalette.FOREST -> if (isDark) ForestDarkScheme else ForestLightScheme
    }

    val boardColors = buildBoardColors(
        scheme = colorScheme,
        palette = palette,
        isDark = isDark,
        colorblindMode = colorblindMode,
    )

    CompositionLocalProvider(
        LocalBoardColors provides boardColors,
        LocalBoardFontScale provides fontScale,
        LocalColorblindMode provides colorblindMode,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}

package com.logicminigames.feature.sudoku.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicminigames.core.data.model.AppLanguage
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.repository.PreferencesPort
import com.logicminigames.core.data.repository.StatsPort
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesPort: PreferencesPort,
    private val statsPort: StatsPort,
) : ViewModel() {

    val preferences: StateFlow<GamePreferences> = preferencesPort.preferences

    fun setThemePalette(palette: AppThemePalette) = viewModelScope.launch {
        preferencesPort.setThemePalette(palette)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        preferencesPort.setThemeMode(mode)
    }

    fun setColorblindMode(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setColorblindMode(enabled)
    }

    fun setBoardFontScale(scale: BoardFontScale) = viewModelScope.launch {
        preferencesPort.setBoardFontScale(scale)
    }

    fun setHighlightPeers(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setHighlightPeers(enabled)
    }

    fun setHighlightSameNumbers(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setHighlightSameNumbers(enabled)
    }

    fun setAutoClearNotes(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setAutoClearNotes(enabled)
    }

    fun setShowRemainingCount(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setShowRemainingCount(enabled)
    }

    fun setErrorCheckMode(mode: ErrorCheckMode) = viewModelScope.launch {
        preferencesPort.setErrorCheckMode(mode)
    }

    fun setHintLimit(limit: Int) = viewModelScope.launch {
        preferencesPort.setHintLimit(limit)
    }

    fun setHintMode(mode: HintMode) = viewModelScope.launch {
        preferencesPort.setHintMode(mode)
    }

    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setSoundEnabled(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesPort.setHapticsEnabled(enabled)
    }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch {
        preferencesPort.setLanguage(language)
    }

    fun resetStats() = viewModelScope.launch {
        statsPort.resetStatsAndAchievements()
    }

    fun resetAllData() = viewModelScope.launch {
        statsPort.resetAllData()
    }
}

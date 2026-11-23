package com.logicminigames.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.logicminigames.core.data.model.AppLanguage
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.repository.PreferencesPort
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.BoardFontScale
import com.logicminigames.core.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "minigames_preferences",
)

class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferencesPort {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val preferences: StateFlow<GamePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs.toGamePreferences() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = GamePreferences(),
        )

    override suspend fun setThemePalette(palette: AppThemePalette) {
        dataStore.edit { it[KEY_THEME_PALETTE] = palette.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setColorblindMode(enabled: Boolean) {
        dataStore.edit { it[KEY_COLORBLIND_MODE] = enabled }
    }

    override suspend fun setBoardFontScale(scale: BoardFontScale) {
        dataStore.edit { it[KEY_BOARD_FONT_SCALE] = scale.name }
    }

    override suspend fun setHighlightPeers(enabled: Boolean) {
        dataStore.edit { it[KEY_HIGHLIGHT_PEERS] = enabled }
    }

    override suspend fun setHighlightSameNumbers(enabled: Boolean) {
        dataStore.edit { it[KEY_HIGHLIGHT_SAME_NUMBERS] = enabled }
    }

    override suspend fun setAutoClearNotes(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_CLEAR_NOTES] = enabled }
    }

    override suspend fun setShowRemainingCount(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_REMAINING_COUNT] = enabled }
    }

    override suspend fun setErrorCheckMode(mode: ErrorCheckMode) {
        dataStore.edit { it[KEY_ERROR_CHECK_MODE] = mode.name }
    }

    override suspend fun setHintLimit(limit: Int) {
        dataStore.edit { it[KEY_HINT_LIMIT] = limit }
    }

    override suspend fun setHintMode(mode: HintMode) {
        dataStore.edit { it[KEY_HINT_MODE] = mode.name }
    }

    override suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    override suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTICS_ENABLED] = enabled }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.tag }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }

    private fun Preferences.toGamePreferences(): GamePreferences = GamePreferences(
        themePalette = enumOrDefault(this[KEY_THEME_PALETTE], AppThemePalette.CLASSIC),
        themeMode = enumOrDefault(this[KEY_THEME_MODE], ThemeMode.SYSTEM),
        colorblindMode = this[KEY_COLORBLIND_MODE] ?: false,
        boardFontScale = enumOrDefault(this[KEY_BOARD_FONT_SCALE], BoardFontScale.NORMAL),
        highlightPeers = this[KEY_HIGHLIGHT_PEERS] ?: true,
        highlightSameNumbers = this[KEY_HIGHLIGHT_SAME_NUMBERS] ?: true,
        autoClearNotes = this[KEY_AUTO_CLEAR_NOTES] ?: true,
        showRemainingCount = this[KEY_SHOW_REMAINING_COUNT] ?: true,
        errorCheckMode = enumOrDefault(this[KEY_ERROR_CHECK_MODE], ErrorCheckMode.IMMEDIATE),
        hintLimit = this[KEY_HINT_LIMIT] ?: 3,
        hintMode = enumOrDefault(this[KEY_HINT_MODE], HintMode.EXPLAIN_FIRST),
        soundEnabled = this[KEY_SOUND_ENABLED] ?: false,
        hapticsEnabled = this[KEY_HAPTICS_ENABLED] ?: false,
        language = AppLanguage.fromTag(this[KEY_LANGUAGE] ?: ""),
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    private companion object {
        val KEY_THEME_PALETTE = stringPreferencesKey("theme_palette")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_COLORBLIND_MODE = booleanPreferencesKey("colorblind_mode")
        val KEY_BOARD_FONT_SCALE = stringPreferencesKey("board_font_scale")
        val KEY_HIGHLIGHT_PEERS = booleanPreferencesKey("highlight_peers")
        val KEY_HIGHLIGHT_SAME_NUMBERS = booleanPreferencesKey("highlight_same_numbers")
        val KEY_AUTO_CLEAR_NOTES = booleanPreferencesKey("auto_clear_notes")
        val KEY_SHOW_REMAINING_COUNT = booleanPreferencesKey("show_remaining_count")
        val KEY_ERROR_CHECK_MODE = stringPreferencesKey("error_check_mode")
        val KEY_HINT_LIMIT = intPreferencesKey("hint_limit")
        val KEY_HINT_MODE = stringPreferencesKey("hint_mode")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
    }
}

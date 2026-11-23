package com.logicminigames.feature.sudoku.stats

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicminigames.core.data.model.AchievementStatus
import com.logicminigames.core.data.model.DailyStatsSummary
import com.logicminigames.core.data.model.DifficultyStats
import com.logicminigames.core.data.repository.StatsPort
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

enum class StatsTab {
    OVERVIEW,
    ACHIEVEMENTS,
}

@Immutable
data class StatsUiState(
    val selectedTab: StatsTab = StatsTab.OVERVIEW,
    val selectedDifficulty: SudokuDifficulty = SudokuDifficulty.EASY,
    val difficultyStats: List<DifficultyStats> = SudokuDifficulty.entries.map { DifficultyStats(difficulty = it) },
    val dailySummary: DailyStatsSummary = DailyStatsSummary(),
    val achievements: List<AchievementStatus> = emptyList(),
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
) {
    val activeDifficultyStats: DifficultyStats
        get() = difficultyStats.firstOrNull { it.difficulty == selectedDifficulty }
            ?: DifficultyStats(difficulty = selectedDifficulty)
}

class StatsViewModel(
    statsPort: StatsPort,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val selectedTab = MutableStateFlow(StatsTab.OVERVIEW)
    private val selectedDifficulty = MutableStateFlow(SudokuDifficulty.EASY)

    val uiState: StateFlow<StatsUiState> = combine(
        selectedTab,
        selectedDifficulty,
        statsPort.observeDifficultyStats(),
        statsPort.observeDailySummary(dateProvider),
        statsPort.observeAchievements(),
    ) { tab, difficulty, stats, daily, achievements ->
        StatsUiState(
            selectedTab = tab,
            selectedDifficulty = difficulty,
            difficultyStats = stats,
            dailySummary = daily,
            achievements = achievements,
            todayEpochDay = dateProvider().toEpochDay(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = StatsUiState(todayEpochDay = dateProvider().toEpochDay()),
    )

    fun selectTab(tab: StatsTab) {
        selectedTab.value = tab
    }

    fun selectDifficulty(difficulty: SudokuDifficulty) {
        selectedDifficulty.value = difficulty
    }
}

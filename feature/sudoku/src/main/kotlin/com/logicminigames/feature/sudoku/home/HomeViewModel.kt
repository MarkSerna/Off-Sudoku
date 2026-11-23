package com.logicminigames.feature.sudoku.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicminigames.core.data.model.ActiveGameSummary
import com.logicminigames.core.data.repository.ActiveGamePort
import com.logicminigames.core.data.repository.StatsPort
import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class HomeUiState(
    val activeGame: ActiveGameSummary? = null,
    val todayDate: LocalDate = LocalDate.now(),
    val todayDifficulty: SudokuDifficulty = SudokuDifficulty.MEDIUM,
    val isDailyCompleted: Boolean = false,
    val dailyCurrentStreak: Int = 0,
)

class HomeViewModel(
    private val activeGamePort: ActiveGamePort,
    private val statsPort: StatsPort,
    private val dailyGenerator: DailyPuzzleGenerator,
    private val dateProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val initialDate = dateProvider()
    private val _uiState = MutableStateFlow(
        HomeUiState(
            todayDate = initialDate,
            todayDifficulty = dailyGenerator.difficultyForDate(initialDate),
        ),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                activeGamePort.activeGameSummary,
                statsPort.observeDailySummary(dateProvider),
            ) { summary, dailyStats ->
                summary to dailyStats
            }.collect { (summary, dailyStats) ->
                val today = dateProvider()
                _uiState.update {
                    it.copy(
                        activeGame = summary,
                        todayDate = today,
                        todayDifficulty = dailyGenerator.difficultyForDate(today),
                        isDailyCompleted = today.toEpochDay() in dailyStats.completedEpochDays,
                        dailyCurrentStreak = dailyStats.currentStreak,
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() {
        val today = dateProvider()
        viewModelScope.launch {
            val completed = activeGamePort.isDailyCompleted(today)
            _uiState.update {
                it.copy(
                    todayDate = today,
                    todayDifficulty = dailyGenerator.difficultyForDate(today),
                    isDailyCompleted = completed,
                )
            }
        }
    }
}

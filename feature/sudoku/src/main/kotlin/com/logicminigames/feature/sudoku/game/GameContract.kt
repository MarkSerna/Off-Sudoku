package com.logicminigames.feature.sudoku.game

import androidx.compose.runtime.Immutable
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.model.CellCoord
import com.logicminigames.core.sudoku.model.LogicalStep
import com.logicminigames.core.sudoku.model.SudokuDifficulty

enum class GameLaunchMode {
    RESUME,
    NEW,
    DAILY,
}

@Immutable
data class CellUiModel(
    val index: Int,
    val row: Int,
    val col: Int,
    val value: Int,
    val isFixed: Boolean,
    val notes: Set<Int>,
    val isSelected: Boolean,
    val isPeerHighlighted: Boolean,
    val isSameNumberHighlighted: Boolean,
    val isError: Boolean,
    val isHintTarget: Boolean,
    val isHintRelated: Boolean,
    val isUnitPulse: Boolean,
)

@Immutable
sealed interface HintBannerUiModel {
    data class Logical(val step: LogicalStep) : HintBannerUiModel
    data class WrongCells(val count: Int) : HintBannerUiModel
    data class DirectReveal(val cell: CellCoord, val digit: Int) : HintBannerUiModel
}

@Immutable
data class GameUiState(
    val isLoading: Boolean = true,
    val difficulty: SudokuDifficulty = SudokuDifficulty.EASY,
    val isDaily: Boolean = false,
    val cells: List<CellUiModel> = emptyList(),
    val selectedIndex: Int? = null,
    val isNotesMode: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val hintsRemaining: Int? = null,
    val elapsedSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val isCompleted: Boolean = false,
    val isNewBestTime: Boolean = false,
    val unlockedAchievements: List<SudokuAchievement> = emptyList(),
    val remainingDigitCounts: Map<Int, Int> = (1..9).associateWith { 9 },
    val errorCheckMode: ErrorCheckMode = ErrorCheckMode.IMMEDIATE,
    val showRemainingCount: Boolean = true,
    val hintMode: HintMode = HintMode.EXPLAIN_FIRST,
    val activeHint: HintBannerUiModel? = null,
    val showHintChoiceSheet: Boolean = false,
    val lastPlacedIndex: Int? = null,
    val placementTick: Int = 0,
    val errorShakeTick: Int = 0,
    val unitPulseTick: Int = 0,
)

sealed interface GameEvent {
    data class SelectCell(val index: Int) : GameEvent
    data class EnterDigit(val digit: Int) : GameEvent
    data object EraseSelected : GameEvent
    data object ToggleNotesMode : GameEvent
    data object Undo : GameEvent
    data object Redo : GameEvent
    data object RequestHint : GameEvent
    data object ChooseExplainHint : GameEvent
    data object ChooseRevealHint : GameEvent
    data object DismissHintSheet : GameEvent
    data object ApplyActiveHint : GameEvent
    data object DismissActiveHint : GameEvent
    data object CheckErrorsOnDemand : GameEvent
    data object TogglePause : GameEvent
    data object PauseFromLifecycle : GameEvent
    data object RestartPuzzle : GameEvent
    data class StartNewPuzzle(val difficulty: SudokuDifficulty) : GameEvent
}

sealed interface GameEffect {
    data object CellSelected : GameEffect
    data object DigitPlaced : GameEffect
    data object NoteToggled : GameEffect
    data object MistakeMade : GameEffect
    data object HintShown : GameEffect
    data object UnitCompleted : GameEffect
    data object GameWon : GameEffect
    data object HintsExhausted : GameEffect
    data class OnDemandCheckResult(val wrongCount: Int) : GameEffect
}

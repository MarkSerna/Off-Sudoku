package com.logicminigames.feature.sudoku.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.engine.HintResult
import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.PuzzlePreloader
import com.logicminigames.core.sudoku.engine.SudokuGameReducer
import com.logicminigames.core.sudoku.engine.SudokuGameState
import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.sudoku.model.SudokuGrid
import com.logicminigames.core.sudoku.model.SudokuPuzzle
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.model.HintMode
import com.logicminigames.core.data.model.SudokuAchievement
import com.logicminigames.core.data.repository.ActiveGamePort
import com.logicminigames.core.data.repository.PreferencesPort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class GameViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val preloader: PuzzlePreloader,
    private val dailyGenerator: DailyPuzzleGenerator,
    private val logicalSolver: LogicalSudokuSolver,
    private val activeGamePort: ActiveGamePort,
    private val preferencesPort: PreferencesPort,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clockTickMs: Long = 1000L,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 16)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private var sessionState: SudokuGameState? = null
    private var selectedIndex: Int? = savedStateHandle.get<Int>(KEY_SELECTED_INDEX)?.takeIf { it in 0..80 }
    private var isNotesMode: Boolean = savedStateHandle.get<Boolean>(KEY_NOTES_MODE) ?: false
    private var isPaused: Boolean = false
    private var isNewBestTime: Boolean = false
    private var unlockedAchievements: List<SudokuAchievement> = emptyList()
    private var activeHint: HintBannerUiModel? = null
    private var showHintChoiceSheet: Boolean = false
    private var onDemandErrorCells: Set<Int> = emptySet()
    private var unitPulseCells: Set<Int> = emptySet()
    private var lastPlacedIndex: Int? = null
    private var placementTick: Int = 0
    private var errorShakeTick: Int = 0
    private var unitPulseTick: Int = 0
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesPort.preferences.collect {
                if (sessionState != null) {
                    publishUiState()
                }
            }
        }
        initializeSession()
    }

    private fun initializeSession() {
        val modeArg = savedStateHandle.get<String>(ARG_MODE)
        val difficultyArg = savedStateHandle.get<String>(ARG_DIFFICULTY)
        val launchMode = modeArg?.let {
            runCatching { GameLaunchMode.valueOf(it) }.getOrNull()
        } ?: GameLaunchMode.RESUME
        val requestedDifficulty = difficultyArg?.let { SudokuDifficulty.fromId(it) } ?: SudokuDifficulty.EASY

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loadedState = when (launchMode) {
                GameLaunchMode.RESUME -> {
                    val saved = activeGamePort.loadActiveGame()
                    if (saved != null) {
                        saved
                    } else {
                        val puzzle = preloader.acquire(requestedDifficulty, viewModelScope)
                        activeGamePort.recordGameStarted(puzzle.difficulty, abandonedPrevious = false)
                        SudokuGameReducer.newGame(puzzle)
                    }
                }

                GameLaunchMode.NEW -> {
                    val hadPrevious = activeGamePort.loadActiveGame() != null
                    val puzzle = preloader.acquire(requestedDifficulty, viewModelScope)
                    activeGamePort.recordGameStarted(puzzle.difficulty, abandonedPrevious = hadPrevious)
                    SudokuGameReducer.newGame(puzzle)
                }

                GameLaunchMode.DAILY -> {
                    val today = LocalDate.now()
                    val existing = activeGamePort.loadActiveGame()
                    if (existing != null && existing.puzzle.dailyEpochDay == today.toEpochDay() && !existing.isCompleted) {
                        existing
                    } else {
                        val hadPrevious = existing != null && !existing.isCompleted
                        val puzzle = withContext(defaultDispatcher) {
                            dailyGenerator.generateForDateAsync(today, defaultDispatcher)
                        }
                        activeGamePort.recordGameStarted(puzzle.difficulty, abandonedPrevious = hadPrevious)
                        SudokuGameReducer.newGame(puzzle)
                    }
                }
            }

            sessionState = loadedState
            isPaused = false
            isNewBestTime = false
            unlockedAchievements = emptyList()
            activeHint = null
            showHintChoiceSheet = false
            onDemandErrorCells = emptySet()
            unitPulseCells = emptySet()
            activeGamePort.saveActiveGame(loadedState)
            publishUiState()
            ensureTimerRunning()
        }
    }

    fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.SelectCell -> handleSelectCell(event.index)
            is GameEvent.EnterDigit -> handleEnterDigit(event.digit)
            GameEvent.EraseSelected -> handleErase()
            GameEvent.ToggleNotesMode -> {
                isNotesMode = !isNotesMode
                savedStateHandle[KEY_NOTES_MODE] = isNotesMode
                publishUiState()
            }

            GameEvent.Undo -> handleUndo()
            GameEvent.Redo -> handleRedo()
            GameEvent.RequestHint -> handleRequestHint()
            GameEvent.ChooseExplainHint -> {
                showHintChoiceSheet = false
                triggerExplainHint()
            }

            GameEvent.ChooseRevealHint -> {
                showHintChoiceSheet = false
                triggerRevealHint()
            }

            GameEvent.DismissHintSheet -> {
                showHintChoiceSheet = false
                publishUiState()
            }

            GameEvent.ApplyActiveHint -> handleApplyActiveHint()
            GameEvent.DismissActiveHint -> {
                activeHint = null
                publishUiState()
            }

            GameEvent.CheckErrorsOnDemand -> handleCheckErrorsOnDemand()
            GameEvent.TogglePause -> {
                val state = sessionState ?: return
                if (state.isCompleted) return
                isPaused = !isPaused
                if (isPaused) {
                    timerJob?.cancel()
                    viewModelScope.launch { activeGamePort.saveActiveGame(state) }
                } else {
                    ensureTimerRunning()
                }
                publishUiState()
            }

            GameEvent.PauseFromLifecycle -> {
                val state = sessionState ?: return
                if (!state.isCompleted && !isPaused) {
                    isPaused = true
                    timerJob?.cancel()
                    viewModelScope.launch { activeGamePort.saveActiveGame(state) }
                    publishUiState()
                }
            }

            GameEvent.RestartPuzzle -> {
                val current = sessionState ?: return
                val restarted = SudokuGameReducer.newGame(current.puzzle)
                resetSessionFlags(restarted)
                ensureTimerRunning()
            }

            is GameEvent.StartNewPuzzle -> {
                viewModelScope.launch {
                    timerJob?.cancel()
                    val hadPrevious = sessionState?.isCompleted == false
                    _uiState.update { it.copy(isLoading = true) }
                    val puzzle = preloader.acquire(event.difficulty, viewModelScope)
                    activeGamePort.recordGameStarted(puzzle.difficulty, abandonedPrevious = hadPrevious)
                    val fresh = SudokuGameReducer.newGame(puzzle)
                    selectedIndex = null
                    savedStateHandle[KEY_SELECTED_INDEX] = null
                    resetSessionFlags(fresh)
                    ensureTimerRunning()
                }
            }
        }
    }

    private fun resetSessionFlags(newState: SudokuGameState) {
        sessionState = newState
        isPaused = false
        isNewBestTime = false
        unlockedAchievements = emptyList()
        activeHint = null
        showHintChoiceSheet = false
        onDemandErrorCells = emptySet()
        unitPulseCells = emptySet()
        lastPlacedIndex = null
        viewModelScope.launch { activeGamePort.saveActiveGame(newState) }
        publishUiState()
    }

    private fun handleSelectCell(index: Int) {
        if (index !in 0 until SudokuTopology.CELL_COUNT || isPaused) return
        selectedIndex = index
        savedStateHandle[KEY_SELECTED_INDEX] = index
        unitPulseCells = emptySet()
        publishUiState()
        _effects.tryEmit(GameEffect.CellSelected)
    }

    private fun handleEnterDigit(digit: Int) {
        val state = sessionState ?: return
        val target = selectedIndex ?: return
        if (isPaused || state.isCompleted) return
        if (SudokuGameReducer.isFixedCell(state, target)) return

        val prefs = preferencesPort.preferences.value
        if (isNotesMode) {
            if (state.currentGrid[target] != 0) return
            val updated = SudokuGameReducer.toggleNote(state, target, digit)
            sessionState = updated
            activeHint = null
            persistAndPublish(updated)
            _effects.tryEmit(GameEffect.NoteToggled)
            return
        }

        val updated = SudokuGameReducer.enterDigit(
            state = state,
            index = target,
            digit = digit,
            errorCheckMode = prefs.errorCheckMode,
            autoClearNotes = prefs.autoClearNotes,
        )
        if (updated === state) return

        sessionState = updated
        activeHint = null
        onDemandErrorCells = onDemandErrorCells - target

        val isMistake = digit != state.puzzle.solution[target]
        if (isMistake && prefs.errorCheckMode == ErrorCheckMode.IMMEDIATE) {
            errorShakeTick++
            unitPulseCells = emptySet()
            _effects.tryEmit(GameEffect.MistakeMade)
        } else {
            lastPlacedIndex = target
            placementTick++
            if (!updated.isCompleted && !isMistake) {
                val newlyCompleted = findCompletedUnitsForCell(updated.currentGrid, target)
                if (newlyCompleted.isNotEmpty()) {
                    unitPulseCells = newlyCompleted
                    unitPulseTick++
                    _effects.tryEmit(GameEffect.UnitCompleted)
                } else {
                    unitPulseCells = emptySet()
                    _effects.tryEmit(GameEffect.DigitPlaced)
                }
            } else {
                unitPulseCells = emptySet()
                _effects.tryEmit(GameEffect.DigitPlaced)
            }
        }

        if (updated.isCompleted) {
            handleVictory(updated)
        } else {
            persistAndPublish(updated)
        }
    }

    private fun handleErase() {
        val state = sessionState ?: return
        val target = selectedIndex ?: return
        if (isPaused || state.isCompleted) return
        val updated = SudokuGameReducer.clearCell(state, target)
        if (updated === state) return
        sessionState = updated
        activeHint = null
        onDemandErrorCells = onDemandErrorCells - target
        unitPulseCells = emptySet()
        persistAndPublish(updated)
    }

    private fun handleUndo() {
        val state = sessionState ?: return
        if (isPaused || state.isCompleted) return
        val updated = SudokuGameReducer.undo(state)
        if (updated === state) return
        sessionState = updated
        activeHint = null
        onDemandErrorCells = emptySet()
        unitPulseCells = emptySet()
        persistAndPublish(updated)
    }

    private fun handleRedo() {
        val state = sessionState ?: return
        if (isPaused || state.isCompleted) return
        val updated = SudokuGameReducer.redo(state)
        if (updated === state) return
        sessionState = updated
        activeHint = null
        onDemandErrorCells = emptySet()
        unitPulseCells = emptySet()
        if (updated.isCompleted) {
            handleVictory(updated)
        } else {
            persistAndPublish(updated)
        }
    }

    private fun handleRequestHint() {
        val state = sessionState ?: return
        if (isPaused || state.isCompleted) return
        val prefs = preferencesPort.preferences.value
        if (prefs.hintLimit >= 0 && state.hintsUsed >= prefs.hintLimit) {
            _effects.tryEmit(GameEffect.HintsExhausted)
            return
        }
        when (prefs.hintMode) {
            HintMode.EXPLAIN_FIRST -> {
                showHintChoiceSheet = true
                publishUiState()
            }

            HintMode.REVEAL_DIRECTLY -> triggerRevealHint()
        }
    }

    private fun triggerExplainHint() {
        val state = sessionState ?: return
        val prefs = preferencesPort.preferences.value
        if (prefs.hintLimit >= 0 && state.hintsUsed >= prefs.hintLimit) {
            _effects.tryEmit(GameEffect.HintsExhausted)
            return
        }

        when (val result = SudokuGameReducer.computeLogicalHint(state, logicalSolver)) {
            is HintResult.LogicalHint -> {
                activeHint = HintBannerUiModel.Logical(result.step)
                result.step.targetCell?.let {
                    selectedIndex = it.index
                    savedStateHandle[KEY_SELECTED_INDEX] = it.index
                }
                _effects.tryEmit(GameEffect.HintShown)
                publishUiState()
            }

            is HintResult.IncorrectCellsFound -> {
                activeHint = HintBannerUiModel.WrongCells(result.cellIndices.size)
                onDemandErrorCells = result.cellIndices
                _effects.tryEmit(GameEffect.HintShown)
                publishUiState()
            }

            is HintResult.FallbackReveal -> {
                activeHint = HintBannerUiModel.DirectReveal(result.cell, result.digit)
                selectedIndex = result.cell.index
                savedStateHandle[KEY_SELECTED_INDEX] = result.cell.index
                _effects.tryEmit(GameEffect.HintShown)
                publishUiState()
            }

            HintResult.BoardAlreadySolved -> Unit
        }
    }

    private fun triggerRevealHint() {
        val state = sessionState ?: return
        val prefs = preferencesPort.preferences.value
        if (prefs.hintLimit >= 0 && state.hintsUsed >= prefs.hintLimit) {
            _effects.tryEmit(GameEffect.HintsExhausted)
            return
        }

        val revealed = SudokuGameReducer.applyRevealHint(
            state = state,
            preferredIndex = selectedIndex,
            autoClearNotes = prefs.autoClearNotes,
        ) ?: return

        val (updated, revealedIndex) = revealed
        sessionState = updated
        selectedIndex = revealedIndex
        savedStateHandle[KEY_SELECTED_INDEX] = revealedIndex
        lastPlacedIndex = revealedIndex
        placementTick++
        activeHint = null
        onDemandErrorCells = onDemandErrorCells - revealedIndex
        _effects.tryEmit(GameEffect.HintShown)

        if (updated.isCompleted) {
            handleVictory(updated)
        } else {
            persistAndPublish(updated)
        }
    }

    private fun handleApplyActiveHint() {
        val state = sessionState ?: return
        val currentHint = activeHint ?: return
        val prefs = preferencesPort.preferences.value
        if (prefs.hintLimit >= 0 && state.hintsUsed >= prefs.hintLimit) {
            activeHint = null
            publishUiState()
            _effects.tryEmit(GameEffect.HintsExhausted)
            return
        }

        val updated = when (currentHint) {
            is HintBannerUiModel.Logical -> {
                if (currentHint.step.isPlacement) {
                    val idx = currentHint.step.targetCell!!.index
                    lastPlacedIndex = idx
                    placementTick++
                    selectedIndex = idx
                    savedStateHandle[KEY_SELECTED_INDEX] = idx
                    SudokuGameReducer.applyLogicalHintStep(
                        state = state,
                        step = currentHint.step,
                        autoClearNotes = prefs.autoClearNotes,
                    )
                } else {
                    // For elimination steps, dismiss banner and reveal next placement cell cleanly
                    val revealed = SudokuGameReducer.applyRevealHint(
                        state = state,
                        preferredIndex = selectedIndex,
                        autoClearNotes = prefs.autoClearNotes,
                    )
                    if (revealed != null) {
                        lastPlacedIndex = revealed.second
                        placementTick++
                        selectedIndex = revealed.second
                        savedStateHandle[KEY_SELECTED_INDEX] = revealed.second
                        revealed.first
                    } else {
                        state
                    }
                }
            }

            is HintBannerUiModel.DirectReveal -> {
                val revealed = SudokuGameReducer.applyRevealHint(
                    state = state,
                    preferredIndex = currentHint.cell.index,
                    autoClearNotes = prefs.autoClearNotes,
                )
                if (revealed != null) {
                    lastPlacedIndex = revealed.second
                    placementTick++
                    revealed.first
                } else {
                    state
                }
            }

            is HintBannerUiModel.WrongCells -> {
                activeHint = null
                publishUiState()
                return
            }
        }

        sessionState = updated
        activeHint = null
        if (updated.isCompleted) {
            handleVictory(updated)
        } else {
            persistAndPublish(updated)
        }
    }

    private fun handleCheckErrorsOnDemand() {
        val state = sessionState ?: return
        if (isPaused || state.isCompleted) return
        val wrong = SudokuGameReducer.findWrongCells(state)
        onDemandErrorCells = wrong
        if (wrong.isNotEmpty()) {
            errorShakeTick++
            _effects.tryEmit(GameEffect.MistakeMade)
        }
        _effects.tryEmit(GameEffect.OnDemandCheckResult(wrong.size))
        publishUiState()
    }

    private fun handleVictory(completedState: SudokuGameState) {
        timerJob?.cancel()
        unitPulseCells = emptySet()
        publishUiState()
        _effects.tryEmit(GameEffect.GameWon)

        viewModelScope.launch {
            val outcome = GameOutcome(
                gameId = KnownGames.SUDOKU,
                difficultyId = completedState.puzzle.difficulty.id,
                elapsedSeconds = completedState.elapsedSeconds,
                mistakes = completedState.mistakes,
                hintsUsed = completedState.hintsUsed,
                isDaily = completedState.puzzle.dailyEpochDay != null,
                won = true,
                completedAtEpochMillis = System.currentTimeMillis(),
            )
            val extras = activeGamePort.recordCompletedGame(
                outcome = outcome,
                dailyEpochDay = completedState.puzzle.dailyEpochDay,
            )
            activeGamePort.clearActiveGame()
            isNewBestTime = extras.isNewBestTime
            unlockedAchievements = extras.unlockedAchievements
            publishUiState()
            preloader.warmUp(
                scope = viewModelScope,
                difficulties = listOf(completedState.puzzle.difficulty),
                targetPerDifficulty = 1,
            )
        }
    }

    private fun ensureTimerRunning() {
        timerJob?.cancel()
        val state = sessionState ?: return
        if (state.isCompleted || isPaused) return

        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(clockTickMs)
                val current = sessionState ?: break
                if (isPaused || current.isCompleted) break
                val ticked = current.copy(elapsedSeconds = current.elapsedSeconds + 1L)
                sessionState = ticked
                if (ticked.elapsedSeconds % 5L == 0L) {
                    activeGamePort.saveActiveGame(ticked)
                }
                publishUiState()
            }
        }
    }

    private fun persistAndPublish(state: SudokuGameState) {
        viewModelScope.launch { activeGamePort.saveActiveGame(state) }
        publishUiState()
    }

    private fun publishUiState() {
        val state = sessionState ?: return
        val prefs = preferencesPort.preferences.value
        val cells = buildCellModels(state, prefs)
        val remainingCounts = SudokuGameReducer.remainingDigitCounts(state, prefs.errorCheckMode)
        val hintsRemaining = if (prefs.hintLimit < 0) {
            null
        } else {
            (prefs.hintLimit - state.hintsUsed).coerceAtLeast(0)
        }

        _uiState.value = GameUiState(
            isLoading = false,
            difficulty = state.puzzle.difficulty,
            isDaily = state.puzzle.dailyEpochDay != null,
            cells = cells,
            selectedIndex = selectedIndex,
            isNotesMode = isNotesMode,
            canUndo = state.undoStack.isNotEmpty() && !state.isCompleted,
            canRedo = state.redoStack.isNotEmpty() && !state.isCompleted,
            mistakes = state.mistakes,
            hintsUsed = state.hintsUsed,
            hintsRemaining = hintsRemaining,
            elapsedSeconds = state.elapsedSeconds,
            isPaused = isPaused,
            isCompleted = state.isCompleted,
            isNewBestTime = isNewBestTime,
            unlockedAchievements = unlockedAchievements,
            remainingDigitCounts = remainingCounts,
            errorCheckMode = prefs.errorCheckMode,
            showRemainingCount = prefs.showRemainingCount,
            hintMode = prefs.hintMode,
            activeHint = activeHint,
            showHintChoiceSheet = showHintChoiceSheet,
            lastPlacedIndex = lastPlacedIndex,
            placementTick = placementTick,
            errorShakeTick = errorShakeTick,
            unitPulseTick = unitPulseTick,
        )
    }

    private fun buildCellModels(
        state: SudokuGameState,
        prefs: GamePreferences,
    ): List<CellUiModel> {
        val sel = selectedIndex
        val selRow = sel?.let { SudokuTopology.rowOf(it) }
        val selCol = sel?.let { SudokuTopology.colOf(it) }
        val selBox = sel?.let { SudokuTopology.boxOf(it) }
        val selValue = sel?.let { state.currentGrid[it] } ?: 0

        val immediateErrors = if (prefs.errorCheckMode == ErrorCheckMode.IMMEDIATE) {
            SudokuGameReducer.findWrongCells(state)
        } else {
            emptySet()
        }
        val displayedErrors = immediateErrors + onDemandErrorCells

        val hintTargetIdx = when (val h = activeHint) {
            is HintBannerUiModel.Logical -> h.step.targetCell?.index
            is HintBannerUiModel.DirectReveal -> h.cell.index
            is HintBannerUiModel.WrongCells, null -> null
        }
        val hintRelatedIndices = when (val h = activeHint) {
            is HintBannerUiModel.Logical -> h.step.relatedCells.mapTo(HashSet()) { it.index }
            is HintBannerUiModel.DirectReveal, is HintBannerUiModel.WrongCells, null -> emptySet()
        }

        return List(SudokuTopology.CELL_COUNT) { idx ->
            val r = SudokuTopology.rowOf(idx)
            val c = SudokuTopology.colOf(idx)
            val b = SudokuTopology.boxOf(idx)
            val value = state.currentGrid[idx]
            val fixed = SudokuGameReducer.isFixedCell(state, idx)
            val isSelected = idx == sel
            val isPeer = prefs.highlightPeers && sel != null && !isSelected &&
                (r == selRow || c == selCol || b == selBox)
            val isSameNum = prefs.highlightSameNumbers && selValue != 0 && !isSelected && value == selValue

            CellUiModel(
                index = idx,
                row = r,
                col = c,
                value = value,
                isFixed = fixed,
                notes = state.notes.notesAt(idx),
                isSelected = isSelected,
                isPeerHighlighted = isPeer,
                isSameNumberHighlighted = isSameNum,
                isError = idx in displayedErrors,
                isHintTarget = idx == hintTargetIdx,
                isHintRelated = idx in hintRelatedIndices && idx != hintTargetIdx,
                isUnitPulse = idx in unitPulseCells,
            )
        }
    }

    private fun findCompletedUnitsForCell(grid: SudokuGrid, idx: Int): Set<Int> {
        val result = HashSet<Int>()
        val units = listOf(
            SudokuTopology.ROW_UNITS[SudokuTopology.rowOf(idx)],
            SudokuTopology.COL_UNITS[SudokuTopology.colOf(idx)],
            SudokuTopology.BOX_UNITS[SudokuTopology.boxOf(idx)],
        )
        for (unit in units) {
            var mask = 0
            var full = true
            for (cellIdx in unit) {
                val v = grid[cellIdx]
                if (v == 0) {
                    full = false
                    break
                }
                mask = mask or SudokuTopology.digitBit(v)
            }
            if (full && mask == SudokuTopology.ALL_DIGITS_MASK) {
                for (cellIdx in unit) result.add(cellIdx)
            }
        }
        return result
    }

    companion object {
        const val ARG_MODE = "mode"
        const val ARG_DIFFICULTY = "difficultyId"
        private const val KEY_SELECTED_INDEX = "selected_index"
        private const val KEY_NOTES_MODE = "notes_mode"
    }
}

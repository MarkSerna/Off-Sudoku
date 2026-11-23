package com.logicminigames.feature.sudoku.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.ui.feedback.GameFeedback
import com.logicminigames.core.ui.icons.AppIcons
import com.logicminigames.core.ui.util.formatElapsedSeconds
import com.logicminigames.feature.sudoku.R
import com.logicminigames.feature.sudoku.game.components.GameActionBar
import com.logicminigames.feature.sudoku.game.components.HintBanner
import com.logicminigames.feature.sudoku.game.components.HintChoiceSheet
import com.logicminigames.feature.sudoku.game.components.NumberPad
import com.logicminigames.feature.sudoku.game.components.PauseOverlay
import com.logicminigames.feature.sudoku.game.components.SudokuBoard
import com.logicminigames.feature.sudoku.game.components.VictoryOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    windowSizeClass: WindowSizeClass,
    feedback: GameFeedback,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onEvent(GameEvent.PauseFromLifecycle)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                GameEffect.CellSelected -> feedback.onCellSelect()
                GameEffect.DigitPlaced -> feedback.onDigitPlace()
                GameEffect.NoteToggled -> feedback.onNoteToggle()
                GameEffect.MistakeMade -> feedback.onMistake()
                GameEffect.HintShown -> feedback.onHint()
                GameEffect.UnitCompleted -> feedback.onUnitCompleted()
                GameEffect.GameWon -> feedback.onVictory()
                GameEffect.HintsExhausted -> {
                    snackbarHostState.showSnackbar(resources.getString(R.string.msg_hints_exhausted))
                }
                is GameEffect.OnDemandCheckResult -> {
                    val msg = if (effect.wrongCount == 0) {
                        resources.getString(R.string.msg_no_errors_found)
                    } else {
                        resources.getString(R.string.msg_errors_found, effect.wrongCount)
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

    val isTwoPane = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact ||
        windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackToHome, modifier = Modifier.size(48.dp)) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                title = {
                    val titleText = if (uiState.isDaily) {
                        stringResource(R.string.game_daily_label, difficultyTitle(uiState.difficulty))
                    } else {
                        difficultyTitle(uiState.difficulty)
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    StatusChip(
                        text = stringResource(R.string.game_mistakes_label, uiState.mistakes),
                        highlightError = uiState.mistakes > 0 && uiState.errorCheckMode == ErrorCheckMode.IMMEDIATE,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val formattedTime = formatElapsedSeconds(uiState.elapsedSeconds)
                    StatusChip(
                        text = formattedTime,
                        contentDescription = stringResource(R.string.cd_timer_value, formattedTime),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.onEvent(GameEvent.TogglePause) },
                        enabled = !uiState.isLoading && !uiState.isCompleted,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (uiState.isPaused) AppIcons.Play else AppIcons.Pause,
                            contentDescription = stringResource(
                                if (uiState.isPaused) R.string.cd_resume else R.string.cd_pause,
                            ),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (isTwoPane) {
                TwoPaneGameContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                )
            } else {
                PortraitGameContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                )
            }

            if (uiState.isPaused && !uiState.isCompleted) {
                PauseOverlay(
                    difficulty = uiState.difficulty,
                    elapsedSeconds = uiState.elapsedSeconds,
                    onResume = { viewModel.onEvent(GameEvent.TogglePause) },
                    onRestart = { viewModel.onEvent(GameEvent.RestartPuzzle) },
                    onHome = onBackToHome,
                )
            }

            if (uiState.isCompleted) {
                VictoryOverlay(
                    difficulty = uiState.difficulty,
                    isDaily = uiState.isDaily,
                    elapsedSeconds = uiState.elapsedSeconds,
                    mistakes = uiState.mistakes,
                    hintsUsed = uiState.hintsUsed,
                    isNewBestTime = uiState.isNewBestTime,
                    unlockedAchievements = uiState.unlockedAchievements,
                    onNewGame = { viewModel.onEvent(GameEvent.StartNewPuzzle(uiState.difficulty)) },
                    onHome = onBackToHome,
                )
            }

            if (uiState.showHintChoiceSheet) {
                HintChoiceSheet(
                    onExplain = { viewModel.onEvent(GameEvent.ChooseExplainHint) },
                    onReveal = { viewModel.onEvent(GameEvent.ChooseRevealHint) },
                    onDismiss = { viewModel.onEvent(GameEvent.DismissHintSheet) },
                )
            }
        }
    }
}

@Composable
private fun PortraitGameContent(
    uiState: GameUiState,
    onEvent: (GameEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SudokuBoard(
            cells = uiState.cells,
            lastPlacedIndex = uiState.lastPlacedIndex,
            placementTick = uiState.placementTick,
            errorShakeTick = uiState.errorShakeTick,
            onCellClick = { onEvent(GameEvent.SelectCell(it)) },
            modifier = Modifier.widthIn(max = 460.dp),
        )

        uiState.activeHint?.let { hint ->
            HintBanner(
                hint = hint,
                onApply = { onEvent(GameEvent.ApplyActiveHint) },
                onDismiss = { onEvent(GameEvent.DismissActiveHint) },
                modifier = Modifier.widthIn(max = 460.dp),
            )
        }

        GameActionBar(
            canUndo = uiState.canUndo,
            canRedo = uiState.canRedo,
            isNotesMode = uiState.isNotesMode,
            hintsRemaining = uiState.hintsRemaining,
            showCheckErrorsButton = uiState.errorCheckMode == ErrorCheckMode.ON_DEMAND,
            onUndo = { onEvent(GameEvent.Undo) },
            onRedo = { onEvent(GameEvent.Redo) },
            onErase = { onEvent(GameEvent.EraseSelected) },
            onToggleNotes = { onEvent(GameEvent.ToggleNotesMode) },
            onHint = { onEvent(GameEvent.RequestHint) },
            onCheckErrors = { onEvent(GameEvent.CheckErrorsOnDemand) },
            modifier = Modifier.widthIn(max = 460.dp),
        )

        NumberPad(
            remainingDigitCounts = uiState.remainingDigitCounts,
            showRemainingCount = uiState.showRemainingCount,
            isNotesMode = uiState.isNotesMode,
            isCompactRows = true,
            onDigitClick = { onEvent(GameEvent.EnterDigit(it)) },
            modifier = Modifier.widthIn(max = 460.dp),
        )
    }
}

@Composable
private fun TwoPaneGameContent(
    uiState: GameUiState,
    onEvent: (GameEvent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            SudokuBoard(
                cells = uiState.cells,
                lastPlacedIndex = uiState.lastPlacedIndex,
                placementTick = uiState.placementTick,
                errorShakeTick = uiState.errorShakeTick,
                onCellClick = { onEvent(GameEvent.SelectCell(it)) },
                modifier = Modifier.widthIn(max = 520.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            uiState.activeHint?.let { hint ->
                HintBanner(
                    hint = hint,
                    onApply = { onEvent(GameEvent.ApplyActiveHint) },
                    onDismiss = { onEvent(GameEvent.DismissActiveHint) },
                )
            }

            GameActionBar(
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                isNotesMode = uiState.isNotesMode,
                hintsRemaining = uiState.hintsRemaining,
                showCheckErrorsButton = uiState.errorCheckMode == ErrorCheckMode.ON_DEMAND,
                onUndo = { onEvent(GameEvent.Undo) },
                onRedo = { onEvent(GameEvent.Redo) },
                onErase = { onEvent(GameEvent.EraseSelected) },
                onToggleNotes = { onEvent(GameEvent.ToggleNotesMode) },
                onHint = { onEvent(GameEvent.RequestHint) },
                onCheckErrors = { onEvent(GameEvent.CheckErrorsOnDemand) },
            )

            NumberPad(
                remainingDigitCounts = uiState.remainingDigitCounts,
                showRemainingCount = uiState.showRemainingCount,
                isNotesMode = uiState.isNotesMode,
                isCompactRows = false,
                onDigitClick = { onEvent(GameEvent.EnterDigit(it)) },
                modifier = Modifier.widthIn(max = 360.dp),
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
    highlightError: Boolean = false,
    contentDescription: String? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (highlightError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (highlightError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

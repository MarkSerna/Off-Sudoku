package com.logicminigames.feature.sudoku

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.logicminigames.core.common.GameOutcome
import com.logicminigames.core.common.KnownGames
import com.logicminigames.core.data.model.GamePreferences
import com.logicminigames.core.data.repository.InMemoryActiveAndStatsPort
import com.logicminigames.core.data.repository.InMemoryPreferencesPort
import com.logicminigames.core.sudoku.engine.DailyPuzzleGenerator
import com.logicminigames.core.sudoku.engine.ErrorCheckMode
import com.logicminigames.core.sudoku.engine.InMemoryPuzzleCacheStore
import com.logicminigames.core.sudoku.engine.LogicalSudokuSolver
import com.logicminigames.core.sudoku.engine.PuzzlePreloader
import com.logicminigames.core.sudoku.engine.SudokuGameReducer
import com.logicminigames.core.sudoku.engine.SudokuGameState
import com.logicminigames.core.sudoku.engine.SudokuGenerator
import com.logicminigames.core.sudoku.engine.SudokuTopology
import com.logicminigames.core.sudoku.model.SudokuDifficulty
import com.logicminigames.core.ui.feedback.NoOpGameFeedback
import com.logicminigames.core.ui.theme.AppThemePalette
import com.logicminigames.core.ui.theme.MinigamesTheme
import com.logicminigames.core.ui.theme.ThemeMode
import com.logicminigames.feature.sudoku.game.GameEvent
import com.logicminigames.feature.sudoku.game.GameLaunchMode
import com.logicminigames.feature.sudoku.game.GameScreen
import com.logicminigames.feature.sudoku.game.GameViewModel
import com.logicminigames.feature.sudoku.home.HomeScreen
import com.logicminigames.feature.sudoku.home.HomeViewModel
import com.logicminigames.feature.sudoku.settings.SettingsScreen
import com.logicminigames.feature.sudoku.settings.SettingsViewModel
import com.logicminigames.feature.sudoku.stats.StatsScreen
import com.logicminigames.feature.sudoku.stats.StatsViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "es-rES-w411dp-h891dp-xxhdpi")
class PlayStoreScreenshotGeneratorTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModelStore = ViewModelStore()
    private val originalLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        Assume.assumeTrue(
            "Screenshot capture runs only when -DcaptureStoreScreenshots=true is set",
            System.getProperty("captureStoreScreenshots") == "true" ||
                System.getenv("CAPTURE_STORE_SCREENSHOTS") == "true",
        )
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Locale.setDefault(originalLocale)
    }

    private fun <T : ViewModel> attach(vm: T, clazz: Class<T>): T =
        ViewModelProvider(
            viewModelStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <V : ViewModel> create(modelClass: Class<V>): V = vm as V
            },
        )[clazz]

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun phoneWindowSize(): WindowSizeClass =
        WindowSizeClass.calculateFromSize(DpSize(411.dp, 891.dp))

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    private fun tabletWindowSize(): WindowSizeClass =
        WindowSizeClass.calculateFromSize(DpSize(1024.dp, 640.dp))

    private fun resolveRawOutputDir(): File {
        var dir: File? = File(".").canonicalFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        val repoRoot = dir ?: error("Could not locate repository root from ${File(".").absolutePath}")
        return File(repoRoot, "docs/store/screenshots/raw").apply { mkdirs() }
    }

    private fun captureAndSave(fileName: String) {
        composeRule.waitForIdle()
        val bitmap: Bitmap = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                composeRule.onRoot().captureToImage().asAndroidBitmap()
            } else {
                drawDecorViewBitmap()
            }
        }.getOrElse {
            drawDecorViewBitmap()
        }

        assertTrue("Captured bitmap width must be > 0", bitmap.width > 0)
        assertTrue("Captured bitmap height must be > 0", bitmap.height > 0)

        val outFile = File(resolveRawOutputDir(), fileName)
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun drawDecorViewBitmap(): Bitmap {
        var result: Bitmap? = null
        composeRule.runOnUiThread {
            val rootView: View = composeRule.activity.window.decorView
            val width = rootView.width.coerceAtLeast(1)
            val height = rootView.height.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            rootView.draw(canvas)
            result = bmp
        }
        return requireNotNull(result)
    }

    private fun seedRealisticProgress(
        difficulty: SudokuDifficulty,
        seed: Long,
        fillCount: Int,
        elapsedSeconds: Long,
    ): SudokuGameState {
        val solver = LogicalSudokuSolver()
        val generator = SudokuGenerator(solver)
        val puzzle = generator.generate(difficulty, seed = seed)
        var state = SudokuGameReducer.newGame(puzzle)

        val empties = (0 until SudokuTopology.CELL_COUNT).filter { puzzle.initialGrid[it] == 0 }
        for (idx in empties.take(fillCount)) {
            state = SudokuGameReducer.enterDigit(
                state = state,
                index = idx,
                digit = puzzle.solution[idx],
                errorCheckMode = ErrorCheckMode.IMMEDIATE,
                autoClearNotes = true,
            )
        }

        val remainingEmpties = empties.drop(fillCount)
        if (remainingEmpties.size >= 2) {
            val noteCellA = remainingEmpties[0]
            val noteCellB = remainingEmpties[1]
            val ansA = puzzle.solution[noteCellA]
            val altA = if (ansA < 9) ansA + 1 else 1
            state = SudokuGameReducer.toggleNote(state, noteCellA, ansA)
            state = SudokuGameReducer.toggleNote(state, noteCellA, altA)
            val ansB = puzzle.solution[noteCellB]
            val altB = if (ansB > 1) ansB - 1 else 9
            state = SudokuGameReducer.toggleNote(state, noteCellB, ansB)
            state = SudokuGameReducer.toggleNote(state, noteCellB, altB)
        }

        return state.copy(elapsedSeconds = elapsedSeconds)
    }

    private suspend fun seedSampleStats(
        statsPort: InMemoryActiveAndStatsPort,
        today: LocalDate,
    ) {
        val todayEpoch = today.toEpochDay()
        for (offset in 5 downTo 0) {
            statsPort.recordCompletedGame(
                GameOutcome(
                    gameId = KnownGames.SUDOKU,
                    difficultyId = SudokuDifficulty.MEDIUM.id,
                    won = true,
                    elapsedSeconds = 215L + offset * 14L,
                    mistakes = if (offset == 0) 0 else 1,
                    hintsUsed = 0,
                    isDaily = true,
                    completedAtEpochMillis = 1_795_000_000_000L + offset * 86_400_000L,
                ),
                dailyEpochDay = todayEpoch - offset,
            )
        }
        statsPort.recordCompletedGame(
            GameOutcome(
                gameId = KnownGames.SUDOKU,
                difficultyId = SudokuDifficulty.EASY.id,
                won = true,
                elapsedSeconds = 142L,
                mistakes = 0,
                hintsUsed = 0,
                isDaily = false,
                completedAtEpochMillis = 1_795_100_000_000L,
            ),
            dailyEpochDay = null,
        )
        statsPort.recordCompletedGame(
            GameOutcome(
                gameId = KnownGames.SUDOKU,
                difficultyId = SudokuDifficulty.HARD.id,
                won = true,
                elapsedSeconds = 398L,
                mistakes = 1,
                hintsUsed = 1,
                isDaily = false,
                completedAtEpochMillis = 1_795_200_000_000L,
            ),
            dailyEpochDay = null,
        )
    }

    @Composable
    private fun ScreenBackdrop(
        palette: AppThemePalette = AppThemePalette.CLASSIC,
        themeMode: ThemeMode = ThemeMode.LIGHT,
        content: @Composable () -> Unit,
    ) {
        MinigamesTheme(palette = palette, themeMode = themeMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                content()
            }
        }
    }

    @Test
    fun capture01_homeScreen_es() {
        val today = LocalDate.of(2026, 11, 22)
        val prefsPort = InMemoryPreferencesPort()
        val statsPort = InMemoryActiveAndStatsPort(prefsPort)
        val state = seedRealisticProgress(SudokuDifficulty.MEDIUM, seed = 8801L, fillCount = 16, elapsedSeconds = 264L)
        runBlocking {
            seedSampleStats(statsPort, today)
            statsPort.saveActiveGame(state)
        }
        val homeVm = attach(
            HomeViewModel(
                activeGamePort = statsPort,
                statsPort = statsPort,
                dailyGenerator = DailyPuzzleGenerator(SudokuGenerator()),
                dateProvider = { today },
            ),
            HomeViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT) {
                HomeScreen(
                    viewModel = homeVm,
                    windowSizeClass = phoneWindowSize(),
                    onContinueGame = {},
                    onStartNewGame = {},
                    onPlayDaily = {},
                    onOpenStats = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.waitUntil(3_000L) { homeVm.uiState.value.activeGame != null }
        captureAndSave("01-home-es.png")
    }

    @Test
    fun capture02_gameClassic_es() {
        val prefsPort = InMemoryPreferencesPort(GamePreferences(themePalette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT))
        val activePort = InMemoryActiveAndStatsPort(prefsPort)
        val state = seedRealisticProgress(SudokuDifficulty.MEDIUM, seed = 202611L, fillCount = 14, elapsedSeconds = 258L)
        val solver = LogicalSudokuSolver()
        val generator = SudokuGenerator(solver)
        runBlocking { activePort.saveActiveGame(state) }

        val firstNonFixedFilled = (0 until SudokuTopology.CELL_COUNT).first {
            state.puzzle.initialGrid[it] == 0 && state.currentGrid[it] != 0
        }

        val gameVm = attach(
            GameViewModel(
                savedStateHandle = SavedStateHandle(mapOf(GameViewModel.ARG_MODE to GameLaunchMode.RESUME.name)),
                preloader = PuzzlePreloader(generator, InMemoryPuzzleCacheStore()),
                dailyGenerator = DailyPuzzleGenerator(generator),
                logicalSolver = solver,
                activeGamePort = activePort,
                preferencesPort = prefsPort,
            ),
            GameViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT) {
                GameScreen(
                    viewModel = gameVm,
                    windowSizeClass = phoneWindowSize(),
                    feedback = NoOpGameFeedback,
                    onBackToHome = {},
                )
            }
        }

        composeRule.waitUntil(3_000L) { !gameVm.uiState.value.isLoading }
        gameVm.onEvent(GameEvent.SelectCell(firstNonFixedFilled))
        composeRule.waitForIdle()
        captureAndSave("02-game-classic-es.png")
    }

    @Test
    fun capture03_gameLogicalHint_es() {
        val prefsPort = InMemoryPreferencesPort(GamePreferences(themePalette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT))
        val activePort = InMemoryActiveAndStatsPort(prefsPort)
        val state = seedRealisticProgress(SudokuDifficulty.HARD, seed = 5150L, fillCount = 10, elapsedSeconds = 312L)
        val solver = LogicalSudokuSolver()
        val generator = SudokuGenerator(solver)
        runBlocking { activePort.saveActiveGame(state) }

        val gameVm = attach(
            GameViewModel(
                savedStateHandle = SavedStateHandle(mapOf(GameViewModel.ARG_MODE to GameLaunchMode.RESUME.name)),
                preloader = PuzzlePreloader(generator, InMemoryPuzzleCacheStore()),
                dailyGenerator = DailyPuzzleGenerator(generator),
                logicalSolver = solver,
                activeGamePort = activePort,
                preferencesPort = prefsPort,
            ),
            GameViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT) {
                GameScreen(
                    viewModel = gameVm,
                    windowSizeClass = phoneWindowSize(),
                    feedback = NoOpGameFeedback,
                    onBackToHome = {},
                )
            }
        }

        composeRule.waitUntil(3_000L) { !gameVm.uiState.value.isLoading }
        gameVm.onEvent(GameEvent.ChooseExplainHint)
        composeRule.waitUntil(3_000L) { gameVm.uiState.value.activeHint != null }
        captureAndSave("03-game-hint-es.png")
    }

    @Test
    fun capture04_gameDarkOcean_es() {
        val prefsPort = InMemoryPreferencesPort(
            GamePreferences(themePalette = AppThemePalette.OCEAN, themeMode = ThemeMode.DARK),
        )
        val activePort = InMemoryActiveAndStatsPort(prefsPort)
        val state = seedRealisticProgress(SudokuDifficulty.EXPERT, seed = 9001L, fillCount = 12, elapsedSeconds = 495L)
        val solver = LogicalSudokuSolver()
        val generator = SudokuGenerator(solver)
        runBlocking { activePort.saveActiveGame(state) }

        val selected = (0 until SudokuTopology.CELL_COUNT).first {
            state.puzzle.initialGrid[it] == 0 && state.currentGrid[it] != 0
        }

        val gameVm = attach(
            GameViewModel(
                savedStateHandle = SavedStateHandle(mapOf(GameViewModel.ARG_MODE to GameLaunchMode.RESUME.name)),
                preloader = PuzzlePreloader(generator, InMemoryPuzzleCacheStore()),
                dailyGenerator = DailyPuzzleGenerator(generator),
                logicalSolver = solver,
                activeGamePort = activePort,
                preferencesPort = prefsPort,
            ),
            GameViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.OCEAN, themeMode = ThemeMode.DARK) {
                GameScreen(
                    viewModel = gameVm,
                    windowSizeClass = phoneWindowSize(),
                    feedback = NoOpGameFeedback,
                    onBackToHome = {},
                )
            }
        }

        composeRule.waitUntil(3_000L) { !gameVm.uiState.value.isLoading }
        gameVm.onEvent(GameEvent.SelectCell(selected))
        composeRule.waitForIdle()
        captureAndSave("04-game-dark-es.png")
    }

    @Test
    fun capture05_statsScreen_es() {
        val today = LocalDate.of(2026, 11, 22)
        val prefsPort = InMemoryPreferencesPort()
        val statsPort = InMemoryActiveAndStatsPort(prefsPort)
        runBlocking { seedSampleStats(statsPort, today) }

        val statsVm = attach(
            StatsViewModel(statsPort = statsPort, dateProvider = { today }),
            StatsViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT) {
                StatsScreen(
                    viewModel = statsVm,
                    onBack = {},
                )
            }
        }

        composeRule.waitUntil(3_000L) { statsVm.uiState.value.dailySummary.totalCompleted > 0 }
        captureAndSave("05-stats-es.png")
    }

    @Test
    fun capture06_settingsScreen_es() {
        val prefsPort = InMemoryPreferencesPort(
            GamePreferences(
                themePalette = AppThemePalette.CLASSIC,
                themeMode = ThemeMode.LIGHT,
                colorblindMode = false,
            ),
        )
        val statsPort = InMemoryActiveAndStatsPort(prefsPort)
        val settingsVm = attach(
            SettingsViewModel(preferencesPort = prefsPort, statsPort = statsPort),
            SettingsViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.CLASSIC, themeMode = ThemeMode.LIGHT) {
                SettingsScreen(
                    viewModel = settingsVm,
                    onBack = {},
                )
            }
        }

        composeRule.waitForIdle()
        captureAndSave("06-settings-es.png")
        composeRule.onNodeWithText("Sudoku · Desarrollado por Altikore").performScrollTo()
        composeRule.onNodeWithText("100 % sin conexión · Sin recopilación de datos").performScrollTo()
        composeRule.waitForIdle()
    }

    @Test
    @Config(sdk = [35], qualifiers = "es-rES-w1024dp-h640dp-xhdpi")
    fun capture07_tabletLandscape_es() {
        val prefsPort = InMemoryPreferencesPort(GamePreferences(themePalette = AppThemePalette.PAPER, themeMode = ThemeMode.LIGHT))
        val activePort = InMemoryActiveAndStatsPort(prefsPort)
        val state = seedRealisticProgress(SudokuDifficulty.MEDIUM, seed = 202611L, fillCount = 15, elapsedSeconds = 189L)
        val solver = LogicalSudokuSolver()
        val generator = SudokuGenerator(solver)
        runBlocking { activePort.saveActiveGame(state) }

        val selected = (0 until SudokuTopology.CELL_COUNT).first {
            state.puzzle.initialGrid[it] == 0 && state.currentGrid[it] != 0
        }

        val gameVm = attach(
            GameViewModel(
                savedStateHandle = SavedStateHandle(mapOf(GameViewModel.ARG_MODE to GameLaunchMode.RESUME.name)),
                preloader = PuzzlePreloader(generator, InMemoryPuzzleCacheStore()),
                dailyGenerator = DailyPuzzleGenerator(generator),
                logicalSolver = solver,
                activeGamePort = activePort,
                preferencesPort = prefsPort,
            ),
            GameViewModel::class.java,
        )

        composeRule.setContent {
            ScreenBackdrop(palette = AppThemePalette.PAPER, themeMode = ThemeMode.LIGHT) {
                GameScreen(
                    viewModel = gameVm,
                    windowSizeClass = tabletWindowSize(),
                    feedback = NoOpGameFeedback,
                    onBackToHome = {},
                )
            }
        }

        composeRule.waitUntil(3_000L) { !gameVm.uiState.value.isLoading }
        gameVm.onEvent(GameEvent.SelectCell(selected))
        composeRule.waitForIdle()
        captureAndSave("07-tablet-landscape-es.png")
    }
}

package com.logicminigames.sudoku

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.logicminigames.core.data.repository.PreferencesPort
import com.logicminigames.core.ui.feedback.AndroidGameFeedback
import com.logicminigames.core.ui.theme.MinigamesTheme
import com.logicminigames.sudoku.navigation.SudokuNavHost
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private val preferencesPort: PreferencesPort by inject()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val prefs by preferencesPort.preferences.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            val feedback = remember(applicationContext) {
                AndroidGameFeedback(
                    context = applicationContext,
                    hapticsEnabledProvider = { preferencesPort.preferences.value.hapticsEnabled },
                    soundEnabledProvider = { preferencesPort.preferences.value.soundEnabled },
                )
            }
            DisposableEffect(feedback) {
                onDispose { feedback.release() }
            }

            MinigamesTheme(
                palette = prefs.themePalette,
                themeMode = prefs.themeMode,
                colorblindMode = prefs.colorblindMode,
                fontScale = prefs.boardFontScale,
            ) {
                SudokuNavHost(
                    windowSizeClass = windowSizeClass,
                    feedback = feedback,
                )
            }
        }
    }
}

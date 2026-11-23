package com.logicminigames.sudoku

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainFlowInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_startsNewGame_andPausesCleanly() {
        composeRule.onNodeWithText("Sudoku").assertIsDisplayed()
        composeRule.onNodeWithTag("sudoku_board").assertDoesNotExist()

        composeRule.onNodeWithTag("home_quick_play").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sudoku_board").assertIsDisplayed()
    }
}

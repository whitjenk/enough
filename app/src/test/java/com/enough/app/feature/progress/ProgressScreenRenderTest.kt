package com.enough.app.feature.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.enough.app.data.model.FeltLevel
import com.enough.app.data.model.WeightTrendDirection
import com.enough.app.domain.progress.DailyFiber
import com.enough.app.ui.theme.EnoughTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProgressScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `progress renders consistency and fiber trend without crashing`() {
        val today = LocalDate.of(2026, 7, 11)
        val state = ProgressUiState(
            fiberSeries = listOf(0, 12, 18, 9, 24, 15, 20).mapIndexed { i, v ->
                DailyFiber(today.minusDays((6 - i).toLong()), v.toDouble())
            },
            fiberTargetG = 28,
            loggedDaySeries = listOf(false, true, true, true, true, true, true),
            daysLoggedLast7 = 6,
            currentWeightKg = 82.0,
            startWeightKg = 84.0,
            targetWeightKg = 80.0,
            weightTrend = WeightTrendDirection.DOWN,
            weeklyActivityMinutes = 90,
            activityGoalMinutes = 150,
            isLoading = false,
        )

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) { ProgressScreen(state) }
        }

        composeRule.onNodeWithText("Logged on 6 of the last 7 days").assertIsDisplayed()
        composeRule.onNodeWithText("Fiber trend").assertIsDisplayed()
    }

    @Test
    fun `a day-one install reports today only, never a window of missed days`() {
        // The regression this guards: a fresh install rendered six hollow dots and
        // "Logged on 1 of the last 7 days", counting days the app did not exist.
        val state = ProgressUiState(
            windowDays = 1,
            loggedDaySeries = listOf(false),
            daysLoggedLast7 = 0,
            checkInFeltSeries = listOf(null),
            isLoading = false,
        )

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) { ProgressScreen(state) }
        }

        composeRule.onNodeWithText("Nothing logged yet today").assertIsDisplayed()
        composeRule.onNodeWithText("Logged on 0 of the last 7 days").assertDoesNotExist()
        composeRule.onNodeWithText("Logged on 0 of the last 1 days").assertDoesNotExist()
    }

    @Test
    fun `day one with something logged says so plainly`() {
        val state = ProgressUiState(
            windowDays = 1,
            loggedDaySeries = listOf(true),
            daysLoggedLast7 = 1,
            checkInFeltSeries = listOf(FeltLevel.STEADY),
            checkInDaysLast7 = 1,
            isLoading = false,
        )

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) { ProgressScreen(state) }
        }

        composeRule.onNodeWithText("Logged today").assertIsDisplayed()
        composeRule.onNodeWithText("You checked in today").assertIsDisplayed()
    }

    @Test
    fun `movement counts days moved, not days logged`() {
        // With no minutes goal the movement line falls back to a day count. It used
        // to read daysLoggedLast7, so logging only a meal claimed a walk.
        val state = ProgressUiState(
            windowDays = 7,
            loggedDaySeries = listOf(false, false, false, false, true, true, true),
            daysLoggedLast7 = 3,
            daysMovedLast7 = 1,
            weeklyActivityMinutes = 0,
            activityGoalMinutes = null,
            isLoading = false,
        )

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) { ProgressScreen(state) }
        }

        // Movement is below the fold, so it is not composed until scrolled to.
        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Movement this week"))

        composeRule.onNodeWithText("Moved on 1 of the last 7 days").assertIsDisplayed()
        composeRule.onNodeWithText("Moved on 3 of the last 7 days").assertDoesNotExist()
    }
}

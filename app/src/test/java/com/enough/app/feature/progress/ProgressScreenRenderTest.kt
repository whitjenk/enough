package com.enough.app.feature.progress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}

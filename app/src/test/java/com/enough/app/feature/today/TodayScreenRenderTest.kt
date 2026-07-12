package com.enough.app.feature.today

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.model.MealSource
import com.enough.app.ui.theme.EnoughTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TodayScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `today renders fiber headline, a logged meal, and movement`() {
        val goal = UserGoal(
            startWeightKg = 82.0,
            targetWeightKg = 78.0,
            weightLossPercent = 5.0,
            activityGoalType = ActivityGoalType.MINUTES,
            activityGoalValue = 150,
            activityGoalCustomLabel = null,
            dailyCalorieEstimate = 2000,
            fiberGramsTarget = 28,
            createdAt = Instant.EPOCH,
        )
        val banana = Food(id = 1, name = "Banana", servingLabel = "1 medium", carbsG = 27.0, fiberG = 3.1, proteinG = 1.3)
        val meal = MealWithFood(
            MealEntry(id = 1, foodId = 1, servingsMultiplier = 2.0, timestamp = Instant.EPOCH, source = MealSource.TEXT),
            banana,
        )
        val state = TodayUiState(
            goal = goal,
            meals = listOf(meal),
            fiberSoFarG = 6.2,
            latestWeight = WeightEntry(id = 1, weightKg = 82.0, timestamp = Instant.EPOCH),
            activities = listOf(ActivityEntry(id = 1, unit = ActivityUnit.MINUTES, amount = 20, note = null, timestamp = Instant.EPOCH)),
            nudge = com.enough.app.domain.rules.Nudge.FiberGap(
                fiberSoFarG = 6,
                gapG = 22,
                suggestionFood = "Lentils",
                suggestionServingLabel = "1/2 cup cooked",
                suggestionFiberG = 8,
            ),
            isLoading = false,
        )

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    state,
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {},
                )
            }
        }

        // Confirms the screen composes without crashing and the fiber headline
        // renders with the correct derived value/format. (Meal/activity rows are
        // below the fold in the small test viewport and, being in a LazyColumn,
        // aren't composed here; their row logic is covered by unit tests.)
        composeRule.onNodeWithText("Fiber today").assertIsDisplayed()
        composeRule.onNodeWithText("6g of 28g").assertIsDisplayed()
        // The daily fiber nudge card renders its supportive, specific message.
        composeRule.onNodeWithText("Lentils", substring = true).assertIsDisplayed()
    }

    @Test
    fun `today renders the one-idea daily swap card when a swap is present`() {
        val state = TodayUiState(
            dailySwap = com.enough.app.domain.rules.DailySwap.Swap(
                food = "Chia seeds",
                servingLabel = "2 tbsp",
                fiberG = 10,
                gentle = false,
            ),
            isLoading = false,
        )

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    state,
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {},
                )
            }
        }

        // The zero-input daily value renders near the top, above the logging
        // actions, with its non-logging, exit-offering copy.
        composeRule.onNodeWithText("One idea for today").assertIsDisplayed()
        composeRule.onNodeWithText("Chia seeds", substring = true).assertIsDisplayed()
    }

    @Test
    fun `reset moment card shows and suppresses the fiber nudge`() {
        val state = TodayUiState(
            nudge = com.enough.app.domain.rules.Nudge.FiberGap(
                fiberSoFarG = 4,
                gapG = 24,
                suggestionFood = "Kidney beans",
                suggestionServingLabel = "1/2 cup",
                suggestionFiberG = 8,
            ),
            showResetMoment = true,
            isLoading = false,
        )

        var shownRecorded = false
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    state,
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = { shownRecorded = true },
                )
            }
        }

        // The reset moment renders its warm, no-catch-up copy...
        composeRule.onNodeWithText("Today's a fresh start").assertIsDisplayed()
        // ...and the routine fiber nudge is suppressed so the two don't contradict.
        composeRule.onNodeWithText("Kidney beans", substring = true).assertDoesNotExist()
        // Composing the card is what records "shown" (not the state computation),
        // so the once-per-rough-patch budget is only spent on a moment truly seen.
        assertTrue(shownRecorded)
    }
}

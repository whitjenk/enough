package com.enough.app.feature.today

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.model.MealSource
import com.enough.app.domain.theme.TimeOfDay
import com.enough.app.ui.theme.EnoughTheme
import org.junit.Assert.assertEquals
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
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                )
            }
        }

        // Confirms the screen composes without crashing and the fiber ring
        // renders with the correct derived value/format. (Meal/activity rows are
        // below the fold in the small test viewport and, being in a LazyColumn,
        // aren't composed here; their row logic is covered by unit tests.)
        composeRule.onNodeWithText("Fiber today").assertIsDisplayed()
        // The hero ring exposes exactly one spoken statement for the whole ring
        // (its inner number/label Texts are cleared from the semantics tree so a
        // screen reader hears one clear value, not three fragments). The tall ring
        // can sit below the small test viewport, so scroll it into view.
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasContentDescription("6 of 28 grams of fiber today"))
        composeRule.onNodeWithContentDescription("6 of 28 grams of fiber today").assertIsDisplayed()
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
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                )
            }
        }

        // The zero-input daily value renders above the logging actions, with its
        // non-logging, exit-offering copy. (Below the fiber ring in the small test
        // viewport, so scroll it into view first.)
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Chia seeds", substring = true))
        composeRule.onNodeWithText("Chia seeds", substring = true).assertIsDisplayed()
        // The exit is the point of this copy — it must survive any re-layout.
        composeRule.onNodeWithText("today can just be today", substring = true).assertIsDisplayed()
    }

    @Test
    fun `today renders the optional felt check-in with its skippable, non-scored copy`() {
        // Minimal state (no swap) so the check-in card sits near the top of the
        // small test viewport. Nothing selected yet -> the note-to-self hint shows.
        val state = TodayUiState(isLoading = false)

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    state,
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                )
            }
        }

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("How did today feel?"))
        composeRule.onNodeWithText("How did today feel?").assertIsDisplayed()
        composeRule.onNodeWithText("Steady").assertIsDisplayed()
        // Framed as a note to yourself, never a nudge to log.
        // Scrolled to directly: the check-in is one tight group now (§7.10 B2), and
        // in the short test viewport its hint sits below the header it belongs to.
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("skip it any day", substring = true))
        composeRule.onNodeWithText("skip it any day", substring = true).assertIsDisplayed()
    }

    @Test
    fun `hide-numbers mode replaces the literal fiber value with a qualitative line`() {
        val goal = UserGoal(
            startWeightKg = null, targetWeightKg = null, weightLossPercent = null,
            activityGoalType = ActivityGoalType.MINUTES, activityGoalValue = 150,
            activityGoalCustomLabel = null, dailyCalorieEstimate = 2000, fiberGramsTarget = 28,
            createdAt = Instant.EPOCH, hideNumbersMode = true,
        )
        val state = TodayUiState(goal = goal, fiberSoFarG = 18.0, isLoading = false)

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    state,
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                )
            }
        }

        // The literal "18g of 28g" is gone; the qualitative stand-in shows instead.
        composeRule.onNodeWithText("Numbers are hidden", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("of 28g", substring = true).assertDoesNotExist()
    }

    @Test
    fun `share today appears only after a check-in`() {
        // No check-in yet: the feeling-first share affordance is absent.
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    TodayUiState(isLoading = false),
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {}, onShareToday = {},
                )
            }
        }
        composeRule.onNodeWithText("Share today").assertDoesNotExist()
    }

    @Test
    fun `share today shows at the check-in peak and fires`() {
        var shared = false
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    TodayUiState(
                        todayFelt = com.enough.app.data.model.FeltLevel.GOOD,
                        isLoading = false,
                    ),
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                    onShareToday = { shared = true },
                )
            }
        }
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Share today"))
        composeRule.onNodeWithText("Share today").performScrollTo().performClick()
        assertTrue(shared)
    }

    @Test
    fun `meal logging is the primary action and weight and movement stay secondary`() {
        // §7.7 item 3: the #1 task sits in the thumb-reachable FAB rather than
        // being one of three equal mid-screen buttons. The FAB is icon-only since
        // §7.9, so its contentDescription is the ONLY thing a screen reader has —
        // asserting on it here is the accessibility guarantee, not a lookup detail.
        var addedMeal = false
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    TodayUiState(isLoading = false),
                    onAddMeal = { addedMeal = true }, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                )
            }
        }

        // The FAB is pinned to the scaffold, so it's reachable without scrolling.
        composeRule.onNodeWithContentDescription("Add a meal").assertIsDisplayed().performClick()
        assertTrue(addedMeal)

        // Weight and movement still exist as secondary entries, not equal thirds.
        // Scrolled to individually: the pair is a FlowRow (§7.10 A3), so in a
        // narrow viewport it wraps onto two lines and the second can sit just
        // off-screen when the first is scrolled into view.
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Log weight"))
        composeRule.onNodeWithText("Log weight").assertIsDisplayed()
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Log movement"))
        composeRule.onNodeWithText("Log movement").assertIsDisplayed()
    }

    @Test
    fun `only one today message renders when the nudge and the swap are both eligible`() {
        // §7.7 item 2: the three messages arbitrate to one on screen rather than
        // stacking. A specific nudge about today outranks the generic swap.
        val state = TodayUiState(
            nudge = com.enough.app.domain.rules.Nudge.FiberGap(
                fiberSoFarG = 6,
                gapG = 22,
                suggestionFood = "Lentils",
                suggestionServingLabel = "1/2 cup cooked",
                suggestionFiberG = 8,
            ),
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
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                )
            }
        }

        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Lentils", substring = true))
        composeRule.onNodeWithText("Lentils", substring = true).assertIsDisplayed()
        // The swap is not merely below the fold — it isn't in the tree at all.
        composeRule.onNodeWithText("Chia seeds", substring = true).assertDoesNotExist()
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
                    onDeleteMeal = {}, onResetMomentShown = { shownRecorded = true }, onCheckIn = {},
                )
            }
        }

        // The reset moment renders its warm, no-catch-up copy (below the hero
        // ring, so scroll it into view in the small test viewport)...
        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("Today's a fresh start"))
        composeRule.onNodeWithText("Today's a fresh start").assertIsDisplayed()
        // ...and the routine fiber nudge is suppressed so the two don't contradict.
        composeRule.onNodeWithText("Kidney beans", substring = true).assertDoesNotExist()
        // Composing the card is what records "shown" (not the state computation),
        // so the once-per-rough-patch budget is only spent on a moment truly seen.
        assertTrue(shownRecorded)
    }

    /**
     * Today has to survive the system font scale, which Android 14+ takes to
     * 200% on Pixels. At 2.0x the check-in chips sat in a plain Row, overflowed
     * it, and Compose resolved that by squeezing them: the first two collapsed
     * to zero width and the third rendered as an 8px sliver, so the whole
     * check-in became untappable. Driving LocalDensity directly keeps this
     * deterministic rather than depending on the test device configuration.
     */
    @Test
    fun `check-in chips stay usable at a 2x font scale`() {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = 2f),
            ) {
                EnoughTheme(dynamicColor = false) {
                    TodayScreen(
                        TodayUiState(isLoading = false),
                        onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                        onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                    )
                }
            }
        }

        composeRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("How did today feel?"))

        // Every option must still be a real, non-degenerate target — the bug was
        // that they existed in the tree but had been squeezed to nothing.
        listOf("A rough one", "Steady", "Good").forEach { label ->
            val width = composeRule.onNodeWithText(label)
                .fetchSemanticsNode().size.width
            assertTrue("\"$label\" collapsed to ${width}px at 2x font scale", width > 0)
        }
    }

    @Test
    fun `the greeting renders the line for the time of day it was given`() {
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                TodayScreen(
                    TodayUiState(isLoading = false),
                    onAddMeal = {}, onLogWeight = {}, onLogActivity = {},
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                    timeOfDay = TimeOfDay.NIGHT,
                )
            }
        }

        // The late-night line offers the exit outright rather than nudging.
        composeRule.onNodeWithText("It's late. Tomorrow is fine too.").assertIsDisplayed()
        composeRule.onNodeWithText("One small thing today is enough.").assertDoesNotExist()
    }

    @Test
    fun `the nudge does not repeat the number the ring already shows`() {
        // The ring says "3 of 28g" directly above; the nudge opening with
        // "You're at 3g of fiber today" made the buddy sound like a readout
        // rather than a friend (§7.10 B2).
        val state = TodayUiState(
            fiberSoFarG = 3.0,
            nudge = com.enough.app.domain.rules.Nudge.FiberGap(
                fiberSoFarG = 3,
                gapG = 25,
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
                    onDeleteMeal = {}, onResetMomentShown = {}, onCheckIn = {},
                    timeOfDay = TimeOfDay.DAY,
                )
            }
        }

        composeRule.onNodeWithText("Lentils", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("You're at", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("of fiber today", substring = true).assertDoesNotExist()
    }
}

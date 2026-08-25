package com.enough.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.enough.app.ui.theme.EnoughTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The ring's states and its motion rule. Haptics are a no-op under Robolectric,
 * so what's pinned here is the visible behaviour: the value shown, the a11y
 * label, and — the one most likely to regress — that opening a screen which
 * already has grams on it does NOT replay the count-up.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FiberRingTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `opening with grams already logged shows the settled value, not a count-up from zero`() {
        // DESIGN.md: no motion on data the person didn't just interact with. If
        // this regresses, the ring animates 0 -> 18 on every app open.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                FiberRing(fiberSoFarG = 18, fiberTargetG = 28, hapticOnIncrease = false)
            }
        }

        // Before any animation frame could run, the real value is already shown.
        composeRule.onNodeWithText("18", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `a new log counts the number up to the new value`() {
        var grams by mutableIntStateOf(6)
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                val target = remember { 28 }
                FiberRing(fiberSoFarG = grams, fiberTargetG = target, hapticOnIncrease = false)
            }
        }
        composeRule.onNodeWithText("6", useUnmergedTree = true).assertIsDisplayed()

        // A meal is logged: the ring settles on the new value (the spring runs in
        // between; we assert the destination, not a specific intermediate frame).
        grams = 14
        composeRule.waitForIdle()
        composeRule.onNodeWithText("14", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("14 of 28 grams of fiber today").assertIsDisplayed()
    }

    @Test
    fun `reaching the target is announced as met`() {
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                FiberRing(fiberSoFarG = 30, fiberTargetG = 28, hapticOnIncrease = false)
            }
        }
        // Over-target: the a11y label says so rather than relying on color alone.
        composeRule
            .onNodeWithContentDescription("30 of 28 grams of fiber today — target reached")
            .assertIsDisplayed()
    }

    @Test
    fun `with no target set the ring reports only what was logged`() {
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                FiberRing(fiberSoFarG = 12, fiberTargetG = 0, hapticOnIncrease = false)
            }
        }
        composeRule
            .onNodeWithContentDescription("12 grams of fiber logged today")
            .assertIsDisplayed()
        composeRule.onNodeWithText("grams", useUnmergedTree = true).assertIsDisplayed()
    }
}

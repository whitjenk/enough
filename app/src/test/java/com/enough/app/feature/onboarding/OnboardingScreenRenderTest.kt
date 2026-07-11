package com.enough.app.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enough.app.ui.theme.EnoughTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM render smoke test: composes the onboarding welcome step and drives the
 * primary action. Catches composition-time crashes and missing resources that a
 * plain compile wouldn't, standing in for an on-device launch.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class OnboardingScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `welcome step renders and get started fires`() {
        var started = false
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = OnboardingStep.WELCOME),
                    onGetStarted = { started = true },
                    onRiskFormChange = {},
                    onSubmitRiskTest = {},
                    onRiskResultContinue = {},
                    onGoalsFormChange = {},
                    onGoalsContinue = {},
                    onConnectHealth = {},
                    onFinish = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Welcome to Enough").assertIsDisplayed()
        composeRule.onNodeWithText("Get started").performClick()

        assertTrue("onGetStarted should have been invoked", started)
    }
}

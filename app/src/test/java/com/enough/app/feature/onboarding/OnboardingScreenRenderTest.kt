package com.enough.app.feature.onboarding

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.enough.app.domain.risk.AgeBand
import com.enough.app.domain.risk.Sex
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
    fun `entry choice renders and both paths fire`() {
        var defaultChosen = false
        var riskChosen = false
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = OnboardingStep.WELCOME),
                    onStartDefault = { defaultChosen = true },
                    onStartRiskTest = { riskChosen = true },
                    onRiskFormChange = {},
                    onSubmitRiskTest = {},
                    onRiskResultContinue = {},
                    onGoalsFormChange = {},
                    onGoalsContinue = {},
                    onExtrasFormChange = {},
                    onExtrasContinue = {},
                    onReminderTimeChange = {},
                    onAcceptReminder = {},
                    onDeclineReminder = {},
                    onConnectHealth = {},
                    onFinish = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Welcome to Enough").assertIsDisplayed()
        composeRule.onNodeWithText("Curious about your risk factors?").performClick()
        assertTrue("onStartRiskTest should have been invoked", riskChosen)

        composeRule.onNodeWithText("Just here to build better habits").performClick()
        assertTrue("onStartDefault should have been invoked", defaultChosen)
    }

    /** Every question answered. Male, so the gestational question isn't asked. */
    private val fullyAnsweredForm = RiskTestForm(
        ageBand = AgeBand.AGE_50_59,
        sex = Sex.MALE,
        familyHistoryDiabetes = true,
        highBloodPressure = false,
        physicallyActive = true,
        weightLbText = "190",
    )

    /** Composes the risk-test step with [form] and no-op callbacks. */
    private fun setRiskTestContent(form: RiskTestForm) {
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = OnboardingStep.RISK_TEST, riskForm = form),
                    onStartDefault = {},
                    onStartRiskTest = {},
                    onRiskFormChange = {},
                    onSubmitRiskTest = {},
                    onRiskResultContinue = {},
                    onGoalsFormChange = {},
                    onGoalsContinue = {},
                    onExtrasFormChange = {},
                    onExtrasContinue = {},
                    onReminderTimeChange = {},
                    onAcceptReminder = {},
                    onDeclineReminder = {},
                    onConnectHealth = {},
                    onFinish = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun `risk test starts with nothing pre-answered and the result locked`() {
        setRiskTestContent(RiskTestForm())

        // Every default used to be the lower-risk answer, which quietly biased
        // the score of anyone who tapped straight through. Nothing is selected
        // until the person selects it.
        composeRule.onAllNodes(isSelectable() and isSelected()).assertCountEquals(0)
        composeRule.onNodeWithText("See my result").assertIsNotEnabled()
    }

    @Test
    fun `one unanswered question still locks see my result`() {
        setRiskTestContent(fullyAnsweredForm.copy(physicallyActive = null))
        composeRule.onNodeWithText("See my result").assertIsNotEnabled()
    }

    @Test
    fun `see my result unlocks once every question is answered`() {
        setRiskTestContent(fullyAnsweredForm)
        composeRule.onNodeWithText("See my result").assertIsEnabled()
    }

    @Test
    fun `reminder offer presents both answers and promises not to ask again`() {
        var accepted = false
        var declined = false

        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState(step = OnboardingStep.REMINDER),
                    onStartDefault = {},
                    onStartRiskTest = {},
                    onRiskFormChange = {},
                    onSubmitRiskTest = {},
                    onRiskResultContinue = {},
                    onGoalsFormChange = {},
                    onGoalsContinue = {},
                    onExtrasFormChange = {},
                    onExtrasContinue = {},
                    onReminderTimeChange = {},
                    onAcceptReminder = { accepted = true },
                    onDeclineReminder = { declined = true },
                    onConnectHealth = {},
                    onFinish = {},
                    onBack = {},
                )
            }
        }

        // Declining has to be a visible, first-class answer — not a skip link —
        // and the promise attached to it is one the app actually keeps.
        composeRule.onNodeWithText("We won't ask again.").assertIsDisplayed()

        composeRule.onNodeWithText("No thanks").performClick()
        assertTrue("onDeclineReminder should have been invoked", declined)

        composeRule.onNodeWithText("Yes, once a day").performClick()
        assertTrue("onAcceptReminder should have been invoked", accepted)
    }
}

package com.enough.app.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.ui.theme.EnoughTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM render smoke test for Settings: confirms the calibration control and the
 * quiet support resource compose without crashing, and that picking a new
 * calibration lean fires the callback with the chosen value.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class SettingsScreenRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `calibration control fires and support resource renders`() {
        var picked: EstimateCalibration? = null
        composeRule.setContent {
            EnoughTheme(dynamicColor = false) {
                SettingsScreen(
                    uiState = SettingsUiState(estimateCalibration = EstimateCalibration.BALANCED),
                    onToggleSync = {},
                    onSetCalibration = { picked = it },
                    onToggleHideNumbers = {},
                    onSetGlp1Stance = {},
                    notificationsBlocked = false,
                    onToggleReminder = {},
                    onSetReminderTime = {},
                    onPrepareFeedback = {},
                    onShareFeedback = {},
                    onDeleteData = {},
                    onOpenPrivacyPolicy = {},
                )
            }
        }

        composeRule.onNodeWithText("Going through a rough patch?").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Lean higher").performScrollTo().performClick()
        assertEquals(EstimateCalibration.HIGH, picked)
    }
}

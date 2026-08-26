package com.enough.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.enough.app.domain.theme.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class BackgroundWashTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `every time of day has its own tint, in both light and dark`() {
        listOf("light" to LightWash, "dark" to DarkWash).forEach { (name, wash) ->
            // A band that shares another band's tint would make the wash
            // pointless for that stretch of the day.
            val tops = TimeOfDayKey.entries.map { wash.getValue(it).top }
            assertEquals("$name: all four bands should be distinct", 4, tops.toSet().size)
        }
    }

    @Test
    fun `the wash always settles into the scheme background`() {
        // The gradient is a gentle settling, not a coloured band: the bottom stop
        // has to match the surface everything else is drawn against.
        TimeOfDayKey.entries.forEach { key ->
            assertEquals("light $key", LightColors.background, LightWash.getValue(key).bottom)
            assertEquals("dark $key", DarkColors.background, DarkWash.getValue(key).bottom)
        }
    }

    @Test
    fun `dark washes stay dark and light washes stay light`() {
        // Guards a transposed map: a light tint in dark mode would blow out the
        // whole screen, and the reverse would look like a rendering fault.
        fun luminanceish(c: androidx.compose.ui.graphics.Color) = c.red + c.green + c.blue
        TimeOfDayKey.entries.forEach { key ->
            assertTrue("light $key too dark", luminanceish(LightWash.getValue(key).top) > 2.5f)
            assertTrue("dark $key too light", luminanceish(DarkWash.getValue(key).top) < 0.5f)
        }
    }

    @Test
    fun `the theme hands down the wash for the time of day it was given`() {
        val timeOfDay = mutableStateOf(TimeOfDay.MORNING)
        var observed: Brush? = null

        composeRule.setContent {
            val current by timeOfDay
            EnoughTheme(darkTheme = false, dynamicColor = false, timeOfDay = current) {
                observed = EnoughTheme.backgroundBrush
            }
        }
        composeRule.waitForIdle()
        val morning = observed

        timeOfDay.value = TimeOfDay.EVENING
        composeRule.waitForIdle()
        val evening = observed

        assertNotEquals("morning and evening should not render the same wash", morning, evening)
    }
}

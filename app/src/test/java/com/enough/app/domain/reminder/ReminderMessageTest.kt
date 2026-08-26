package com.enough.app.domain.reminder

import androidx.test.core.app.ApplicationProvider
import com.enough.app.R
import com.enough.app.domain.rules.DailySwap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderMessageTest {

    private fun swap(gentle: Boolean = false, bridge: Boolean = false) = DailySwap.Swap(
        food = "Black beans",
        servingLabel = "1/2 cup",
        fiberG = 7,
        gentle = gentle,
        bridge = bridge,
    )

    @Test
    fun `plain swap gets the plain tone`() {
        assertEquals(ReminderTone.PLAIN, ReminderMessage.toneFor(swap(), hideNumbers = false))
    }

    @Test
    fun `glp1 tones are chosen ahead of plain, with bridge the most specific`() {
        assertEquals(
            ReminderTone.GENTLE,
            ReminderMessage.toneFor(swap(gentle = true), hideNumbers = false),
        )
        assertEquals(
            ReminderTone.BRIDGE,
            ReminderMessage.toneFor(swap(gentle = true, bridge = true), hideNumbers = false),
        )
    }

    @Test
    fun `hide-numbers outranks every other tone`() {
        // A lock screen is often not private. No combination of settings may
        // route a person's hidden gram count onto one.
        for (gentle in listOf(false, true)) {
            for (bridge in listOf(false, true)) {
                assertEquals(
                    "gentle=$gentle bridge=$bridge",
                    ReminderTone.HIDDEN,
                    ReminderMessage.toneFor(swap(gentle, bridge), hideNumbers = true),
                )
            }
        }
    }

    @Test
    fun `the number-free bodies carry no numeric placeholder`() {
        // Structural: the template itself must have nowhere to put a quantity,
        // so no future edit can reintroduce one without failing here.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val numberFree = listOf(
            R.string.reminder_body_hidden,
            R.string.reminder_body_gentle,
            R.string.reminder_body_bridge,
        )
        for (res in numberFree) {
            val template = context.getString(res, "food", "serving")
            assertFalse("\"$template\" should carry no numeric placeholder", template.contains("%3"))
            assertFalse("\"$template\" should carry no numeral", template.any { it.isDigit() })
        }
    }

    @Test
    fun `only the plain body states a quantity`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val plain = context.getString(R.string.reminder_body, "Black beans", "half a cup", 7)
        assertTrue("the plain body should name the grams", plain.contains("7"))
    }
}

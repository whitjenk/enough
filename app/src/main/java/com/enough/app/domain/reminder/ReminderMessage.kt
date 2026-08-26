package com.enough.app.domain.reminder

import com.enough.app.domain.rules.DailySwap

/** Which register the reminder speaks in. The UI maps these to actual strings. */
enum class ReminderTone {
    /** The ordinary case: names the food and roughly what it adds. */
    PLAIN,

    /** On or coming off a GLP-1: smaller, softer, no number to hit. */
    GENTLE,

    /** Coming off a GLP-1 specifically: fiber as the satiety bridge. */
    BRIDGE,

    /** Hide-numbers mode: names the food, states no quantity at all. */
    HIDDEN,
}

/**
 * Chooses the reminder's register (SPEC §7.8). Pure and unit-tested, separately
 * from the worker that does the posting.
 *
 * [HIDDEN][ReminderTone.HIDDEN] deliberately outranks every other tone. A
 * reminder lands on a lock screen, which is frequently visible to people other
 * than its owner — so a gram count someone explicitly chose to hide must not be
 * able to reach it by any combination of settings.
 */
object ReminderMessage {

    fun toneFor(swap: DailySwap.Swap, hideNumbers: Boolean): ReminderTone = when {
        hideNumbers -> ReminderTone.HIDDEN
        swap.bridge -> ReminderTone.BRIDGE
        swap.gentle -> ReminderTone.GENTLE
        else -> ReminderTone.PLAIN
    }
}

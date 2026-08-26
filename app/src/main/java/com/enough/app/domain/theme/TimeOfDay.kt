package com.enough.app.domain.theme

import java.time.LocalTime

/**
 * Which part of the day it is, used to tint the app's background wash
 * (§7.10 B1).
 *
 * **This tracks the clock and nothing else.** It must never be derived from
 * fiber totals, logging streaks, or any other measure of how the person is
 * doing. A background that shifts when someone is behind is an evaluative
 * signal — the shame pattern in a new costume — and the app forbids that
 * (CLAUDE.md). Morning-warm to evening-deep is neutral: it says "it's evening",
 * not "you're failing".
 *
 * Boundaries are deliberately coarse. The point is that opening the app at 7am
 * and at 10pm should not feel identical, not that the tint tracks sunrise.
 */
enum class TimeOfDay {
    MORNING,
    DAY,
    EVENING,
    NIGHT,
    ;

    companion object {
        /** The part of the day [time] falls in. */
        fun at(time: LocalTime): TimeOfDay = when (time.hour) {
            in 5..10 -> MORNING
            in 11..16 -> DAY
            in 17..20 -> EVENING
            else -> NIGHT // 21:00-04:59
        }

        /** The part of the day it is right now, in the device's own time zone. */
        fun now(): TimeOfDay = at(LocalTime.now())
    }
}

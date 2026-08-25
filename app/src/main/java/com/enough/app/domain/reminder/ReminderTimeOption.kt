package com.enough.app.domain.reminder

/**
 * The handful of times the reminder can be set to (SPEC §7.8).
 *
 * A short preset list rather than a clock picker, deliberately: this choice
 * appears in onboarding, where every extra interaction is a place to drop out,
 * and "roughly when" is all the precision a once-a-day note needs. Someone who
 * wants 4:07pm is not being served by being asked.
 *
 * Pure — no Android, no string resources. The UI supplies the labels.
 */
enum class ReminderTimeOption(val minuteOfDay: Int) {
    MORNING(9 * 60),
    MIDDAY(12 * 60),
    AFTERNOON(16 * 60),
    EVENING(20 * 60),
    ;

    companion object {
        /** The default: late enough to be true about the day, early enough to act on. */
        val DEFAULT = AFTERNOON

        /** The option closest to [minuteOfDay], for rendering a stored value. */
        fun nearest(minuteOfDay: Int): ReminderTimeOption =
            entries.minBy { kotlin.math.abs(it.minuteOfDay - minuteOfDay) }
    }
}

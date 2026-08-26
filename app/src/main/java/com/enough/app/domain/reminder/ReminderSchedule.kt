package com.enough.app.domain.reminder

/**
 * How often the daily reminder is still allowed to speak.
 *
 * The order is one-directional under [ReminderSchedule.afterFiring] — daily
 * quiets to weekly, weekly quiets to stopped, and nothing here ever moves back
 * the other way. Only the person returning on their own does that.
 */
enum class ReminderCadence { DAILY, WEEKLY, STOPPED }

/**
 * Decides whether the on-device daily reminder may fire today, and how quickly
 * it backs off when nobody is listening (SPEC §7.8).
 *
 * **This class is where the app's promise about notifications actually lives.**
 * `CLAUDE.md` is explicit: never escalate toward a quiet user, the check-in
 * fires once and genuinely stops, silence is allowed to just be silence. Every
 * retention notification in this category does the opposite — it gets louder and
 * more frequent as someone drifts away. So the rule here is inverted: **the
 * longer the app goes unopened, the less it speaks, until it stops entirely.**
 *
 * A stopped reminder does not resume on a timer, an update, or a milestone. It
 * resumes only when the person opens the app of their own accord, which
 * [afterAppOpened] treats as the single signal that they want to be here.
 *
 * Pure and testable — no Android, no Room, no clock. The caller supplies today.
 */
object ReminderSchedule {

    /** After this many reminders with no app open, drop from daily to weekly. */
    const val UNOPENED_BEFORE_WEEKLY = 5

    /** After this many in total, stop entirely and wait to be invited back. */
    const val UNOPENED_BEFORE_STOP = 8

    /** Days between fires once backed off to [ReminderCadence.WEEKLY]. */
    const val WEEKLY_INTERVAL_DAYS = 7

    /**
     * The cadence earned by [unopenedCount] consecutive reminders that were
     * followed by no app open. Counts at or past [UNOPENED_BEFORE_STOP] mean the
     * app has said enough.
     */
    fun cadenceFor(unopenedCount: Int): ReminderCadence = when {
        unopenedCount >= UNOPENED_BEFORE_STOP -> ReminderCadence.STOPPED
        unopenedCount >= UNOPENED_BEFORE_WEEKLY -> ReminderCadence.WEEKLY
        else -> ReminderCadence.DAILY
    }

    /**
     * Whether a reminder may be posted today.
     *
     * Note the second guard: if the person has **already opened the app today**,
     * no reminder fires at all. A reminder is for someone who hasn't been here —
     * nudging someone who already showed up would be talking for the sake of it.
     *
     * @param enabled the person's own opt-in; false means silence, always
     * @param unopenedCount consecutive reminders since the last app open
     * @param lastFiredEpochDay when a reminder last posted, or null if never
     * @param lastOpenedEpochDay when the app was last opened, or null if never
     * @param todayEpochDay today, as a local epoch day
     */
    fun shouldFire(
        enabled: Boolean,
        unopenedCount: Int,
        lastFiredEpochDay: Long?,
        lastOpenedEpochDay: Long?,
        todayEpochDay: Long,
    ): Boolean {
        if (!enabled) return false
        if (lastOpenedEpochDay == todayEpochDay) return false
        return when (cadenceFor(unopenedCount)) {
            ReminderCadence.STOPPED -> false
            ReminderCadence.DAILY -> lastFiredEpochDay != todayEpochDay
            ReminderCadence.WEEKLY ->
                lastFiredEpochDay == null ||
                    todayEpochDay - lastFiredEpochDay >= WEEKLY_INTERVAL_DAYS
        }
    }

    /** The new unopened count after a reminder posts. Only ever grows. */
    fun afterFiring(unopenedCount: Int): Int = unopenedCount + 1

    /**
     * The new unopened count after the person opens the app: zero, always.
     *
     * This is the only path back to [ReminderCadence.DAILY], and it is
     * deliberately generous — someone who returns after a long silence is not
     * made to re-earn anything, exactly as a missed day never has to be made up
     * anywhere else in this app.
     */
    fun afterAppOpened(): Int = 0
}

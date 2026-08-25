package com.enough.app.feature.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.enough.app.EnoughApplication
import com.enough.app.R
import com.enough.app.di.AppContainer
import com.enough.app.domain.reminder.ReminderMessage
import com.enough.app.domain.reminder.ReminderSchedule
import com.enough.app.domain.reminder.ReminderTone
import com.enough.app.domain.rules.DailySwap
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Posts (or deliberately declines to post) the daily reminder, then enqueues
 * tomorrow's run. SPEC §7.8.
 *
 * The decision of *whether* to speak lives in [ReminderSchedule] and the
 * decision of *what to say* lives in [DailySwap] — both pure and unit-tested.
 * This class only does I/O and wiring.
 *
 * Two cases are treated as "we had nothing to say" rather than "they ignored
 * us", and so do not count toward the back-off:
 * 1. **No safe suggestion exists** for this person's restrictions. The buddy
 *    voice requires reacting to something specific and real; a generic "don't
 *    forget to log" is exactly the notification this app refuses to send.
 * 2. **The OS refused the post.** That is not a signal about the person.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as? EnoughApplication)?.container
            ?: return Result.success()

        try {
            fireIfDue(container)
        } finally {
            // Always queue the next day, even if today's attempt threw — a
            // transient failure must not silently end the reminder forever.
            val minuteOfDay = container.userPreferencesRepository.reminderMinuteOfDay.first()
            if (container.userPreferencesRepository.reminderEnabled.first()) {
                ReminderScheduler.schedule(applicationContext, minuteOfDay, ZonedDateTime.now())
            }
        }
        return Result.success()
    }

    private suspend fun fireIfDue(container: AppContainer) {
        val prefs = container.userPreferencesRepository
        val today = LocalDate.now().toEpochDay()

        val due = ReminderSchedule.shouldFire(
            enabled = prefs.reminderEnabled.first(),
            unopenedCount = prefs.reminderUnopenedCount.first(),
            lastFiredEpochDay = prefs.reminderLastFiredEpochDay.first(),
            lastOpenedEpochDay = prefs.lastOpenedEpochDay.first(),
            todayEpochDay = today,
        )
        if (!due) return

        val goal = container.goalRepository.getGoal()
        val swap = DailySwap.forDay(
            epochDay = today,
            candidates = container.foodRepository.topFiberFoods(),
            restrictions = goal?.dietaryRestrictions ?: emptySet(),
            otherRestriction = goal?.dietaryRestrictionOther,
            gentle = goal?.gentleFiber ?: false,
            comingOff = goal?.comingOffGlp1 ?: false,
        ) ?: return

        val body = bodyFor(swap, hideNumbers = goal?.hideNumbersMode ?: false)
        if (ReminderNotifier.post(applicationContext, body)) {
            prefs.recordReminderFired(today)
        }
    }

    /**
     * The reminder's words. Every variant ends with a real way out — the
     * notification is an offer, never an instruction (CLAUDE.md buddy voice).
     * Which register to use is decided by [ReminderMessage]; this only formats.
     */
    private fun bodyFor(swap: DailySwap.Swap, hideNumbers: Boolean): String =
        when (ReminderMessage.toneFor(swap, hideNumbers)) {
            ReminderTone.HIDDEN -> applicationContext.getString(
                R.string.reminder_body_hidden,
                swap.food,
                swap.servingLabel,
            )
            ReminderTone.BRIDGE -> applicationContext.getString(
                R.string.reminder_body_bridge,
                swap.food,
                swap.servingLabel,
            )
            ReminderTone.GENTLE -> applicationContext.getString(
                R.string.reminder_body_gentle,
                swap.food,
                swap.servingLabel,
            )
            ReminderTone.PLAIN -> applicationContext.getString(
                R.string.reminder_body,
                swap.food,
                swap.servingLabel,
                swap.fiberG,
            )
        }
}

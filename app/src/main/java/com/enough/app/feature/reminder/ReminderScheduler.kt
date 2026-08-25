package com.enough.app.feature.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Schedules the daily reminder (SPEC §7.8).
 *
 * Uses a chain of one-time work requests — each run enqueues the next — rather
 * than `PeriodicWorkRequest`, because a periodic request cannot be pinned to a
 * time of day, and rather than `AlarmManager`, because exact alarms would mean
 * requesting `SCHEDULE_EXACT_ALARM`. A reminder that can drift by a few minutes
 * is not worth a special permission prompt.
 *
 * All of this is local to the device: no push service, no server, no account.
 */
object ReminderScheduler {

    const val WORK_NAME = "daily_reminder"

    /**
     * Enqueue the next reminder for [minuteOfDay], replacing any pending one.
     * Safe to call repeatedly (launch, settings change, after each fire).
     */
    fun schedule(
        context: Context,
        minuteOfDay: Int,
        now: ZonedDateTime = ZonedDateTime.now(),
    ) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayUntilNext(minuteOfDay, now))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancel any pending reminder — used when the person turns them off. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Time from [now] until the next occurrence of [minuteOfDay] local time.
     * Rolls to tomorrow when today's time has already passed. Pure and testable.
     */
    fun delayUntilNext(minuteOfDay: Int, now: ZonedDateTime): Duration {
        val target = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        val todayAt = now.with(target)
        val next = if (todayAt.isAfter(now)) {
            todayAt
        } else {
            now.with(LocalDate.from(now).plusDays(1)).with(target)
        }
        return Duration.between(now, next)
    }
}

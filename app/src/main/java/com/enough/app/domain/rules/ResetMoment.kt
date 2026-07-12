package com.enough.app.domain.rules

/**
 * The "Enough" moment — the reset-day decision (SPEC §7.5 / Phase 1 §12), the
 * single feature most tied to the app's own name. It decides whether to show a
 * warm, no-catch-up "today's a fresh start" card after a rough patch.
 *
 * Pure and testable. Two triggers:
 *  - **Absence:** it's been [ABSENCE_TRIGGER_DAYS]+ days since the last log.
 *  - **Wide miss:** the last completed day was logged but finished under
 *    [WIDE_MISS_FRACTION] of the fiber target (computed by the caller).
 *
 * Crucially it obeys the "never escalate toward a quiet user" rule (CLAUDE.md):
 * it fires at most once per quiet/rough episode and then genuinely stops until
 * the person logs again. Silence is allowed to just be silence.
 */
object ResetMoment {

    /** Days since the last log that count as a real gap worth a gentle reset. */
    const val ABSENCE_TRIGGER_DAYS = 3

    /** Below this fraction of the day's fiber target counts as a "wide" miss. */
    const val WIDE_MISS_FRACTION = 0.5

    /**
     * Minimum days between wide-miss-triggered moments. Unlike the absence
     * trigger (whose episode ends only when the person logs again), a wide miss
     * re-arms with every new log — someone logging daily but consistently under
     * target would otherwise see the card every other day, turning the app's
     * warmest moment into a formula.
     */
    const val WIDE_MISS_COOLDOWN_DAYS = 7

    /**
     * @param todayEpochDay today as a local epoch-day
     * @param daysSinceLastLog whole days since the last log; null = never logged
     * @param hadWideMissYesterday the last completed day was logged but fell far
     *   short of the fiber target (caller computes against [WIDE_MISS_FRACTION])
     * @param lastShownEpochDay the day a reset moment was last shown, or null
     * @return whether to show the reset-day card now
     */
    fun shouldShow(
        todayEpochDay: Long,
        daysSinceLastLog: Int?,
        hadWideMissYesterday: Boolean,
        lastShownEpochDay: Long?,
    ): Boolean {
        // Already shown today: keep it visible for the rest of the day rather
        // than flickering off on the next recomposition/re-subscription.
        if (lastShownEpochDay == todayEpochDay) return true

        val absence = daysSinceLastLog != null && daysSinceLastLog >= ABSENCE_TRIGGER_DAYS
        if (!absence && !hadWideMissYesterday) return false

        // Fire at most once per episode. Suppress if we've already shown a reset
        // moment at or after the person's last activity; a new log moves the
        // last-log day forward and re-enables it for a genuinely new rough patch.
        if (lastShownEpochDay != null) {
            val lastLogEpochDay = daysSinceLastLog?.let { todayEpochDay - it }
            if (lastLogEpochDay == null || lastShownEpochDay >= lastLogEpochDay) return false

            // The wide-miss path additionally rate-limits itself (see
            // [WIDE_MISS_COOLDOWN_DAYS]); an absence gap is a distinct-enough
            // episode that the once-per-episode rule above is sufficient.
            if (!absence && todayEpochDay - lastShownEpochDay < WIDE_MISS_COOLDOWN_DAYS) return false
        }
        return true
    }
}

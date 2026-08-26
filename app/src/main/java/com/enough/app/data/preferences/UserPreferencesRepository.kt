package com.enough.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Small key/value app preferences backed by DataStore. Local-only, like
 * everything else. Holds routing/UX flags (not health data): whether onboarding
 * is finished, whether Health Connect syncing is enabled, and the daily
 * reminder's settings and back-off state (SPEC §7.8).
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    val healthConnectSyncEnabled: Flow<Boolean> =
        dataStore.data.map { it[HEALTH_CONNECT_SYNC_ENABLED] ?: true }

    /**
     * The local epoch-day a reset-day ("Enough") moment was last shown, or null.
     * Kept here as a UX/session flag rather than on `RulesEngineState` (which is
     * only a cache of the latest rules-engine result); this also avoids a Room
     * migration for a single suppression flag.
     */
    val resetMomentShownEpochDay: Flow<Long?> =
        dataStore.data.map { it[RESET_MOMENT_SHOWN_EPOCH_DAY] }

    /**
     * Whether the person has ever shared a card (SPEC §7.6 Step 3). A single
     * aggregate boolean — no count, no timestamp — so it can feed the anonymous
     * feedback summary without ever revealing when or how often.
     */
    val everSharedCard: Flow<Boolean> =
        dataStore.data.map { it[EVER_SHARED_CARD] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setHealthConnectSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[HEALTH_CONNECT_SYNC_ENABLED] = enabled }
    }

    suspend fun setResetMomentShownEpochDay(epochDay: Long) {
        dataStore.edit { it[RESET_MOMENT_SHOWN_EPOCH_DAY] = epochDay }
    }

    suspend fun setEverSharedCard() {
        dataStore.edit { it[EVER_SHARED_CARD] = true }
    }

    /**
     * Whether the person opted into the daily reminder. Defaults to **false** —
     * the reminder is offered once during onboarding and is never on unless it
     * was actually chosen (SPEC §7.8 item 6).
     */
    val reminderEnabled: Flow<Boolean> =
        dataStore.data.map { it[REMINDER_ENABLED] ?: false }

    /** Local time of day for the reminder, as minutes past midnight. */
    val reminderMinuteOfDay: Flow<Int> =
        dataStore.data.map { it[REMINDER_MINUTE_OF_DAY] ?: DEFAULT_REMINDER_MINUTE_OF_DAY }

    /**
     * Whether the one-time onboarding offer has been made. Once true, the app
     * never asks again — declining a reminder is a real answer, not the opening
     * of a negotiation (CLAUDE.md: never escalate toward a quiet user).
     */
    val reminderOfferShown: Flow<Boolean> =
        dataStore.data.map { it[REMINDER_OFFER_SHOWN] ?: false }

    /** Consecutive reminders posted with no app open since. Drives the back-off. */
    val reminderUnopenedCount: Flow<Int> =
        dataStore.data.map { it[REMINDER_UNOPENED_COUNT] ?: 0 }

    /** Local epoch-day a reminder last posted, or null if one never has. */
    val reminderLastFiredEpochDay: Flow<Long?> =
        dataStore.data.map { it[REMINDER_LAST_FIRED_EPOCH_DAY] }

    /** Local epoch-day the app was last opened, or null if not recorded yet. */
    val lastOpenedEpochDay: Flow<Long?> =
        dataStore.data.map { it[LAST_OPENED_EPOCH_DAY] }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderMinuteOfDay(minuteOfDay: Int) {
        dataStore.edit { it[REMINDER_MINUTE_OF_DAY] = minuteOfDay }
    }

    suspend fun setReminderOfferShown() {
        dataStore.edit { it[REMINDER_OFFER_SHOWN] = true }
    }

    /** Record that a reminder posted: stamp the day and count it as unanswered. */
    suspend fun recordReminderFired(epochDay: Long) {
        dataStore.edit {
            it[REMINDER_LAST_FIRED_EPOCH_DAY] = epochDay
            it[REMINDER_UNOPENED_COUNT] = (it[REMINDER_UNOPENED_COUNT] ?: 0) + 1
        }
    }

    /**
     * Record that the person opened the app, which clears the back-off entirely.
     * This is the only thing that un-quiets a reminder that has backed off or
     * stopped — see [com.enough.app.domain.reminder.ReminderSchedule].
     */
    suspend fun recordAppOpened(epochDay: Long) {
        dataStore.edit {
            it[LAST_OPENED_EPOCH_DAY] = epochDay
            it[REMINDER_UNOPENED_COUNT] = 0
        }
    }

    /** Wipe all preferences (used by "Delete my data"). */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        /**
         * 4pm. Late enough that the day's logging has mostly happened, early
         * enough that today's swap is still actionable — dinner and a grocery
         * stop are both still ahead. A morning reminder would arrive before
         * there is anything true to say about the day.
         */
        const val DEFAULT_REMINDER_MINUTE_OF_DAY = 16 * 60

        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val HEALTH_CONNECT_SYNC_ENABLED = booleanPreferencesKey("health_connect_sync_enabled")
        private val RESET_MOMENT_SHOWN_EPOCH_DAY = longPreferencesKey("reset_moment_shown_epoch_day")
        private val EVER_SHARED_CARD = booleanPreferencesKey("ever_shared_card")
        private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
        private val REMINDER_OFFER_SHOWN = booleanPreferencesKey("reminder_offer_shown")
        private val REMINDER_UNOPENED_COUNT = intPreferencesKey("reminder_unopened_count")
        private val REMINDER_LAST_FIRED_EPOCH_DAY = longPreferencesKey("reminder_last_fired_epoch_day")
        private val LAST_OPENED_EPOCH_DAY = longPreferencesKey("last_opened_epoch_day")
    }
}

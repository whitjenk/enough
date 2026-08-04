package com.enough.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Small key/value app preferences backed by DataStore. Local-only, like
 * everything else. Holds routing/UX flags (not health data): whether onboarding
 * is finished and whether Health Connect syncing is enabled.
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

    /** Wipe all preferences (used by "Delete my data"). */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HEALTH_CONNECT_SYNC_ENABLED = booleanPreferencesKey("health_connect_sync_enabled")
        val RESET_MOMENT_SHOWN_EPOCH_DAY = longPreferencesKey("reset_moment_shown_epoch_day")
        val EVER_SHARED_CARD = booleanPreferencesKey("ever_shared_card")
    }
}

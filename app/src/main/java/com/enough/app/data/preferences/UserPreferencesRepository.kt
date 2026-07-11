package com.enough.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setHealthConnectSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[HEALTH_CONNECT_SYNC_ENABLED] = enabled }
    }

    /** Wipe all preferences (used by "Delete my data"). */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HEALTH_CONNECT_SYNC_ENABLED = booleanPreferencesKey("health_connect_sync_enabled")
    }
}

package com.enough.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant

/** Availability of Health Connect on this device. */
enum class HealthConnectAvailability {
    /** Installed and usable. */
    AVAILABLE,

    /** Supported, but the Health Connect provider needs installing/updating. */
    UPDATE_REQUIRED,

    /** Not supported on this device — the app must work fully without it. */
    NOT_SUPPORTED,
}

/**
 * Thin wrapper over the Health Connect SDK. Phase 0 reads steps, sleep, and
 * weight (reads land in the Today task); onboarding only needs the availability
 * check and the permission-request flow.
 *
 * Health Connect is strictly optional — the app is fully usable if it is
 * unavailable or its permissions are denied (CLAUDE.md error-handling rule).
 */
class HealthConnectManager(private val context: Context) {

    /** The read permissions the app asks for. Read-only in Phase 0. */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }

    /** The Health Connect client, or null if it can't be created on this device. */
    fun clientOrNull(): HealthConnectClient? =
        if (availability() == HealthConnectAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }

    /** Whether every requested permission has already been granted. */
    suspend fun hasAllPermissions(): Boolean {
        val client = clientOrNull() ?: return false
        return client.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    /**
     * The ActivityResult contract used to request permissions from a composable
     * via `rememberLauncherForActivityResult`.
     */
    fun requestPermissionsContract() =
        PermissionController.createRequestPermissionResultContract()

    // --- Reads (all null-safe: unavailable, denied, or error all yield null) ---

    /** Total steps in [start, end), or null if Health Connect can't provide it. */
    suspend fun readSteps(start: Instant, end: Instant): Long? {
        val client = clientOrNull() ?: return null
        if (!hasAllPermissions()) return null
        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
            response[StepsRecord.COUNT_TOTAL]
        } catch (e: Exception) {
            null
        }
    }

    /** Total sleep minutes across sessions overlapping [start, end), or null. */
    suspend fun readSleepMinutes(start: Instant, end: Instant): Long? {
        val client = clientOrNull() ?: return null
        if (!hasAllPermissions()) return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                ),
            )
            response.records
                .sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                .takeIf { response.records.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}

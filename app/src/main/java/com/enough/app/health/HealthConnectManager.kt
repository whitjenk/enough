package com.enough.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

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
}

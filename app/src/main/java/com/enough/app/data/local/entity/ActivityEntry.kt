package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.enough.app.data.model.ActivityUnit
import java.time.Instant

/**
 * A logged bout of activity. [unit] mirrors the goal type chosen in onboarding
 * so a person with a minutes/steps/custom goal logs in the same terms — the
 * non-step accessibility path from SPEC.md §5 flows all the way through.
 *
 * [amount] is interpreted by [unit] (minutes, steps, or a custom count); [note]
 * holds the self-description for a CUSTOM activity.
 */
@Entity(
    tableName = "activity_entry",
    indices = [Index("timestamp")],
)
data class ActivityEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unit: ActivityUnit,
    val amount: Int,
    val note: String?,
    val timestamp: Instant,
)

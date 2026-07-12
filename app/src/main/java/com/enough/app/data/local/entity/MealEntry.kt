package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.enough.app.data.model.MealEntryType
import com.enough.app.data.model.MealSource
import java.time.Instant

/**
 * A logged meal: a reference to a [Food] plus how many servings. The food's
 * nutrients are multiplied by [servingsMultiplier] to get the meal's contribution.
 */
@Entity(
    tableName = "meal_entry",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("foodId"), Index("timestamp")],
)
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodId: Long,
    val servingsMultiplier: Double,
    val timestamp: Instant,
    val source: MealSource,
    /** How precise this entry is; drives honest range display for coarse logs. */
    val entryType: MealEntryType = MealEntryType.DATABASE_MATCHED,
)

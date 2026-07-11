package com.enough.app.data.local

import androidx.room.TypeConverter
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.model.MealSource
import com.enough.app.data.model.NudgeType
import com.enough.app.data.model.RiskResultSource
import com.enough.app.data.model.WeightTrendDirection
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters. Timestamps use java.time (available on minSdk 26) and
 * are stored as epoch values; enums are stored by name so the DB stays readable
 * and stable if enum ordinals ever change.
 */
class Converters {
    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun mealSourceToName(value: MealSource?): String? = value?.name

    @TypeConverter
    fun nameToMealSource(value: String?): MealSource? = value?.let(MealSource::valueOf)

    @TypeConverter
    fun activityGoalTypeToName(value: ActivityGoalType?): String? = value?.name

    @TypeConverter
    fun nameToActivityGoalType(value: String?): ActivityGoalType? = value?.let(ActivityGoalType::valueOf)

    @TypeConverter
    fun activityUnitToName(value: ActivityUnit?): String? = value?.name

    @TypeConverter
    fun nameToActivityUnit(value: String?): ActivityUnit? = value?.let(ActivityUnit::valueOf)

    @TypeConverter
    fun riskSourceToName(value: RiskResultSource?): String? = value?.name

    @TypeConverter
    fun nameToRiskSource(value: String?): RiskResultSource? = value?.let(RiskResultSource::valueOf)

    @TypeConverter
    fun weightTrendToName(value: WeightTrendDirection?): String? = value?.name

    @TypeConverter
    fun nameToWeightTrend(value: String?): WeightTrendDirection? = value?.let(WeightTrendDirection::valueOf)

    @TypeConverter
    fun nudgeTypeToName(value: NudgeType?): String? = value?.name

    @TypeConverter
    fun nameToNudgeType(value: String?): NudgeType? = value?.let(NudgeType::valueOf)
}

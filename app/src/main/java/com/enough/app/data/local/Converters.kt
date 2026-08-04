package com.enough.app.data.local

import androidx.room.TypeConverter
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.data.model.DietaryTag
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.FeltLevel
import com.enough.app.data.model.Glp1Stance
import com.enough.app.data.model.MealEntryType
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
    fun mealEntryTypeToName(value: MealEntryType?): String? = value?.name

    @TypeConverter
    fun nameToMealEntryType(value: String?): MealEntryType? = value?.let(MealEntryType::valueOf)

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

    @TypeConverter
    fun feltLevelToName(value: FeltLevel?): String? = value?.name

    @TypeConverter
    fun nameToFeltLevel(value: String?): FeltLevel? = value?.let(FeltLevel::valueOf)

    @TypeConverter
    fun glp1StanceToName(value: Glp1Stance?): String? = value?.name

    @TypeConverter
    fun nameToGlp1Stance(value: String?): Glp1Stance? = value?.let(Glp1Stance::valueOf)

    /** Stored as a comma-separated list of enum names; empty set -> "". */
    @TypeConverter
    fun dietaryRestrictionsToString(value: Set<DietaryRestriction>?): String? =
        value?.joinToString(separator = ",") { it.name }

    @TypeConverter
    fun stringToDietaryRestrictions(value: String?): Set<DietaryRestriction>? =
        value?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { DietaryRestriction.valueOf(it) }
            ?.toSet()

    @TypeConverter
    fun estimateCalibrationToName(value: EstimateCalibration?): String? = value?.name

    @TypeConverter
    fun nameToEstimateCalibration(value: String?): EstimateCalibration? =
        value?.let(EstimateCalibration::valueOf)

    /** Stored as a comma-separated list of enum names; empty set -> "". */
    @TypeConverter
    fun dietaryTagsToString(value: Set<DietaryTag>?): String? =
        value?.joinToString(separator = ",") { it.name }

    @TypeConverter
    fun stringToDietaryTags(value: String?): Set<DietaryTag>? =
        value?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { DietaryTag.valueOf(it) }
            ?.toSet()
}

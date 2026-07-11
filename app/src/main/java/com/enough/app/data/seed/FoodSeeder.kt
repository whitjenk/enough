package com.enough.app.data.seed

import android.content.Context
import com.enough.app.data.local.dao.FoodDao
import com.enough.app.data.local.entity.Food
import kotlinx.serialization.json.Json

/**
 * Loads the bundled food list into Room once, on first launch.
 *
 * Parsing ([parseFoods]) is a pure function with no Android/Room dependency so
 * it can be unit-tested directly; [seedIfEmpty] does the I/O and DB write and is
 * a no-op if the table is already populated (idempotent across launches).
 */
class FoodSeeder(
    private val json: Json = DEFAULT_JSON,
) {
    /** Parse the foods JSON document into [Food] entities. Pure and testable. */
    fun parseFoods(jsonText: String): List<Food> =
        json.decodeFromString<List<SeedFood>>(jsonText).map(SeedFood::toEntity)

    /**
     * Seed the food table from [assetPath] if it is currently empty. Safe to
     * call on every launch — returns the number of foods inserted (0 if the
     * table was already seeded).
     */
    suspend fun seedIfEmpty(
        context: Context,
        dao: FoodDao,
        assetPath: String = ASSET_PATH,
    ): Int {
        if (dao.count() > 0) return 0
        val jsonText = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        val foods = parseFoods(jsonText)
        dao.insertAll(foods)
        return foods.size
    }

    companion object {
        const val ASSET_PATH = "foods.json"
        private val DEFAULT_JSON = Json { ignoreUnknownKeys = true }
    }
}

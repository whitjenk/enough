package com.enough.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.enough.app.data.local.entity.DailyCheckIn
import com.enough.app.data.model.FeltLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/**
 * The daily check-in is one row per calendar day: re-tapping a different answer
 * the same day replaces it (upsert by date), and a range query returns the days
 * that were actually checked in — nothing is invented for skipped days.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyCheckInDaoTest {

    private lateinit var db: EnoughDatabase
    private val dao get() = db.dailyCheckInDao()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnoughDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `re-tapping the same day replaces rather than stacks`() = runBlocking {
        val day = LocalDate.of(2026, 8, 3)
        dao.upsert(DailyCheckIn(day, FeltLevel.ROUGH, Instant.EPOCH))
        dao.upsert(DailyCheckIn(day, FeltLevel.GOOD, Instant.EPOCH.plusSeconds(60)))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(FeltLevel.GOOD, all.single().felt)
        assertEquals(FeltLevel.GOOD, dao.observeForDate(day).first()?.felt)
    }

    @Test
    fun `observeForDate is null on a skipped day and the range returns only real check-ins`() = runBlocking {
        val monday = LocalDate.of(2026, 8, 3)
        val wednesday = LocalDate.of(2026, 8, 5)
        dao.upsert(DailyCheckIn(monday, FeltLevel.STEADY, Instant.EPOCH))
        dao.upsert(DailyCheckIn(wednesday, FeltLevel.GOOD, Instant.EPOCH))

        // Tuesday was skipped — no penalty, just no row.
        assertNull(dao.observeForDate(LocalDate.of(2026, 8, 4)).first())

        val week = dao.observeBetween(monday, LocalDate.of(2026, 8, 9)).first()
        assertEquals(listOf(monday, wednesday), week.map { it.date })
    }
}

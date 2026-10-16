package com.aljaz.weighttracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aljaz.weighttracker.data.local.WeightDatabase
import com.aljaz.weighttracker.data.local.WeightEntryDao
import com.aljaz.weighttracker.data.local.WeightEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests for [WeightEntryDao] using a fresh in-memory database for each test.
 */
@RunWith(AndroidJUnit4::class)
class WeightEntryDaoTest {

    private lateinit var db: WeightDatabase
    private lateinit var dao: WeightEntryDao

    /** Creates an empty in-memory database before every test. */
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, WeightDatabase::class.java).build()
        dao = db.weightEntryDao()
    }

    /** Closes the database after every test. */
    @After
    fun tearDown() {
        db.close()
    }

    /** Saving twice on the same day must keep only the latest value. */
    @Test
    fun upsert_sameDay_replacesEntry() = runBlocking<Unit> {
        val day = LocalDate.of(2026, 10, 16).toEpochDay()

        dao.upsert(WeightEntryEntity(day, 80.0))
        dao.upsert(WeightEntryEntity(day, 79.5))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(79.5, all[0].weightKg, 0.001)
    }

    /** Entries must come back sorted oldest-first, regardless of insert order. */
    @Test
    fun observeAll_returnsEntriesSortedByDate() = runBlocking<Unit> {
        val day1 = LocalDate.of(2026, 10, 14).toEpochDay()
        val day2 = LocalDate.of(2026, 10, 15).toEpochDay()
        val day3 = LocalDate.of(2026, 10, 16).toEpochDay()

        dao.upsertAll(
            listOf(
                WeightEntryEntity(day3, 79.0),
                WeightEntryEntity(day1, 81.0),
                WeightEntryEntity(day2, 80.0),
            )
        )

        val result = dao.observeAll().first()
        assertEquals(listOf(day1, day2, day3), result.map { it.dateEpochDay })
    }

    /** Deleting a day removes only that day's entry. */
    @Test
    fun deleteByDate_removesOnlyThatEntry() = runBlocking<Unit> {
        val day1 = LocalDate.of(2026, 10, 15).toEpochDay()
        val day2 = LocalDate.of(2026, 10, 16).toEpochDay()
        dao.upsertAll(listOf(WeightEntryEntity(day1, 80.0), WeightEntryEntity(day2, 79.5)))

        dao.deleteByDate(day1)

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertTrue(all.none { it.dateEpochDay == day1 })
    }
}
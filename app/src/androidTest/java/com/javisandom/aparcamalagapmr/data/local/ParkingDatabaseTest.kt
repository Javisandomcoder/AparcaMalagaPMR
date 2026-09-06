package com.javisandom.aparcamalagapmr.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ParkingDatabaseTest {
    private lateinit var database: ParkingDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ParkingDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun replace_all_removes_stale_spots_atomically() = runBlocking {
        val dao = database.parkingSpotDao()
        dao.replaceAll(
            listOf(
                ParkingSpotEntity("old", "Antigua", 36.7, -4.4, "", 1),
                ParkingSpotEntity("keep", "Conservada", 36.71, -4.41, "", 1),
            ),
        )

        dao.replaceAll(
            listOf(
                ParkingSpotEntity("keep", "Actualizada", 36.72, -4.42, "Nueva", 2),
            ),
        )

        assertEquals(
            listOf(ParkingSpotEntity("keep", "Actualizada", 36.72, -4.42, "Nueva", 2)),
            dao.getAll(),
        )
    }
}

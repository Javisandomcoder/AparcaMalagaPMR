package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CarParkingSelectionTest {
    @Test
    fun `car list is deterministic and limited to six spots`() {
        val spots = (7 downTo 1).map { index ->
            ParkingSpot(
                id = index.toString(),
                address = "CALLE ${index.toString().padStart(2, '0')}",
                latitude = 36.72,
                longitude = -4.42,
            )
        }

        val selected = spotsForCar(spots)

        assertEquals(6, selected.size)
        assertEquals(listOf("1", "2", "3", "4", "5", "6"), selected.map { it.id })
    }

    @Test
    fun `car list uses nearest spots when a location exists`() {
        val origin = UserLocation(36.72, -4.42)
        val far = ParkingSpot("far", "A", 36.78, -4.55)
        val near = ParkingSpot("near", "Z", 36.7201, -4.42)

        assertEquals(
            listOf("near", "far"),
            spotsForCar(listOf(far, near), origin = origin).map { it.id },
        )
    }
}

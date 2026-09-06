package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingDistanceTest {
    private val origin = UserLocation(latitude = 36.7213, longitude = -4.4214)

    @Test
    fun `distance is zero for the same coordinates`() {
        val spot = ParkingSpot("1", "Origen", origin.latitude, origin.longitude)

        assertEquals(0.0, distanceMeters(origin, spot), 0.01)
    }

    @Test
    fun `distance uses geographic coordinates`() {
        val spot = ParkingSpot("1", "Norte", 36.7223, -4.4214)

        assertTrue(distanceMeters(origin, spot) in 110.0..112.0)
    }

    @Test
    fun `spots are sorted nearest first`() {
        val far = ParkingSpot("far", "Lejos", 36.75, -4.50)
        val near = ParkingSpot("near", "Cerca", 36.7214, -4.4214)

        assertEquals(listOf("near", "far"), sortParkingSpotsByDistance(listOf(far, near), origin).map { it.id })
    }

    @Test
    fun `distance is formatted for meters and kilometres`() {
        assertEquals("850 m", formatDistance(850.0))
        assertEquals("1,3 km", formatDistance(1_250.0))
    }
}

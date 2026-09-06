package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ParkingSpotTest {
    @Test
    fun `display name uses the known address`() {
        val spot = ParkingSpot(
            id = "pmr-1",
            address = "Paseo del Parque",
            latitude = 36.7196,
            longitude = -4.4166,
        )

        assertEquals("Paseo del Parque", spot.displayName)
    }

    @Test
    fun `display name falls back when address is blank`() {
        val spot = ParkingSpot(
            id = "pmr-2",
            address = "   ",
            latitude = 36.7213,
            longitude = -4.4214,
        )

        assertEquals("Plaza PMR", spot.displayName)
    }
}

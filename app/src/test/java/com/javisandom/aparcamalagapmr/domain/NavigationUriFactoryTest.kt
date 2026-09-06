package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationUriFactoryTest {
    @Test
    fun `creates a geo URI with coordinates and encoded label`() {
        val spot = ParkingSpot("1", "CALLE ÁLAVA, 18", 36.72532804, -4.362277)

        assertEquals(
            "geo:36.72532804,-4.362277?q=36.72532804,-4.362277(CALLE+%C3%81LAVA%2C+18)",
            navigationUriFor(spot),
        )
    }
}
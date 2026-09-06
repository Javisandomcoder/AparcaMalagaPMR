package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ParkingSpotSearchTest {
    private val spots = listOf(
        ParkingSpot("1", "AVENIDA DE JUAN SEBASTIÁN ELCANO, 193", 36.72, -4.36, "Esquina con Valparaíso", 2),
        ParkingSpot("2", "CALLE HUNGRÍA, 10", 36.71, -4.45, "Junto al mercado", 1),
    )

    @Test
    fun `search ignores case and accents`() {
        assertEquals(listOf("1"), searchParkingSpots(spots, "valparaiso").map { it.id })
        assertEquals(listOf("2"), searchParkingSpots(spots, "hungria").map { it.id })
    }

    @Test
    fun `blank search returns all spots`() {
        assertEquals(spots, searchParkingSpots(spots, "  "))
    }
}
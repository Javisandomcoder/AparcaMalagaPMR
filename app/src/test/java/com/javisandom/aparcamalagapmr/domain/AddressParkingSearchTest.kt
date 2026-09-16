package com.javisandom.aparcamalagapmr.domain

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AddressParkingSearchTest {
    private val spots = listOf(ParkingSpot("a", "Calle Uno", 36.72, -4.40), ParkingSpot("b", "Calle Dos", 36.72, -4.44))
    @Test fun `address takes precedence over matching street names`() = runBlocking {
        val destination = ResolvedAddress("Calle Uno 100", UserLocation(36.72, -4.44))
        val result = searchParkingNearAddress(spots, "Calle Uno") { destination }
        assertEquals("b", result.spots.first().id)
        assertEquals(destination.location, result.origin)
        assertEquals(destination.label, result.address)
    }
    @Test fun `unknown address never falls back to vehicle or fictitious origin`() = runBlocking {
        val result = searchParkingNearAddress(spots, "Desconocida") { null }
        assertTrue(result.spots.isEmpty()); assertNull(result.origin); assertNotNull(result.message)
    }
    @Test fun `short input avoids geocoding`() = runBlocking {
        searchParkingNearAddress(spots, "a") { fail("No geocoding for incomplete query"); null }; Unit
    }
    @Test fun `cancellation is propagated`() = runBlocking {
        try {
            searchParkingNearAddress(spots, "Calle Uno") { throw CancellationException() }
            fail("Cancellation swallowed")
        } catch (_: CancellationException) { }
    }
}

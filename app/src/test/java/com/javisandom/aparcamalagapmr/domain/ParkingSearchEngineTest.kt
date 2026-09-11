package com.javisandom.aparcamalagapmr.domain

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ParkingSearchEngineTest {
    private val spots = listOf(
        ParkingSpot("1", "CALLE HUNGRÍA, 10", 36.71, -4.45),
        ParkingSpot("2", "PASEO CERRADO DE CALDERÓN, 18", 36.72615397, -4.38156389),
    )

    @Test fun `local matches ignore accents and do not call geocoder`() = runBlocking {
        val result = ParkingSearchEngine(spots).search("calderon", null) { error("Unexpected geocoding") }
        assertEquals(listOf("2"), result.spots.map { it.id })
    }

    @Test fun `unmatched address orders all spots from address not device`() = runBlocking {
        val destination = ResolvedAddress("Dirección de prueba", UserLocation(36.726, -4.38))
        val result = ParkingSearchEngine(spots).search("dirección sin plaza", UserLocation(36.71, -4.45)) { destination }
        assertEquals(listOf("2", "1"), result.spots.map { it.id })
        assertEquals(destination.location, result.origin)
        assertEquals(destination.label, result.address)
    }

    @Test fun `unknown address does not invent an origin or use device as fallback`() = runBlocking {
        val result = ParkingSearchEngine(spots).search("no existe", UserLocation(36.71, -4.45)) { null }
        assertTrue(result.spots.isEmpty())
        assertNull(result.origin)
        assertNotNull(result.message)
    }

    @Test fun `blank search orders by device and does not geocode`() = runBlocking {
        val result = ParkingSearchEngine(spots).search(" ", UserLocation(36.726, -4.38)) { error("Unexpected geocoding") }
        assertEquals(listOf("2", "1"), result.spots.map { it.id })
        assertNull(result.address)
    }

    @Test fun `cancelled address lookup cannot publish late results`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var published = false
        val job = launch {
            ParkingSearchEngine(spots).search("no match", null) {
                started.complete(Unit)
                withContext(NonCancellable) { delay(30) }
                ResolvedAddress("Old query", UserLocation(36.72, -4.4))
            }
            published = true
        }
        withTimeout(1_000) { started.await() }
        job.cancelAndJoin()
        assertFalse(published)
    }
}

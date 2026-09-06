package com.javisandom.aparcamalagapmr.car

import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class CarParkingLoaderTest {
    private val spots = listOf(ParkingSpot("77785", "PASEO CERRADO DE CALDERÓN, 18", 36.72615397, -4.38156389))

    @Test fun `loading remains observable while read is suspended then content arrives`() = runBlocking {
        val pending = CompletableDeferred<List<ParkingSpot>>()
        val loader = CarParkingLoader { pending.await() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { loader.load() }
        assertEquals(CarParkingState.Loading, loader.state.value)
        assertFalse(job.isCompleted)
        pending.complete(spots)
        job.join()
        assertEquals(CarParkingState.Ready(spots), loader.state.value)
    }

    @Test fun `read failure becomes error and retry can recover`() = runBlocking {
        var fail = true
        val loader = CarParkingLoader { if (fail) error("read failed") else spots }
        loader.load()
        assertEquals(CarParkingState.Error, loader.state.value)
        fail = false
        loader.load()
        assertEquals(CarParkingState.Ready(spots), loader.state.value)
    }

    @Test fun `cancellation propagates instead of becoming an error`() = runBlocking {
        val loader = CarParkingLoader { throw CancellationException("screen destroyed") }
        try {
            loader.load()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(CarParkingState.Loading, loader.state.value)
        }
    }
}

package com.javisandom.aparcamalagapmr.data

import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ParkingRepositoryTest {
    @Test
    fun `first load seeds empty storage from bundled data`() = runBlocking {
        val storage = FakeParkingStorage()
        val repository = ParkingRepository(
            storage = storage,
            bundledSource = ParkingJsonSource { oneSpotJson("bundled") },
            remoteSource = ParkingJsonSource { error("network must not be used for initial load") },
            refreshState = FakeRefreshState(),
            nowMillis = { 10L },
        )

        val spots = repository.loadCachedOrBundled()

        assertEquals(listOf("bundled"), spots.map { it.id })
        assertEquals(spots, storage.read())
    }

    @Test
    fun `failed refresh preserves the last valid cache`() = runBlocking {
        val storage = FakeParkingStorage()
        val repository = ParkingRepository(
            storage = storage,
            bundledSource = ParkingJsonSource { oneSpotJson("cached") },
            remoteSource = ParkingJsonSource { "not valid json" },
            refreshState = FakeRefreshState(),
            nowMillis = { 20L },
        )
        repository.loadCachedOrBundled()

        val result = repository.refresh(force = true)

        assertEquals(true, result is ParkingRefreshResult.Failed)
        assertEquals(listOf("cached"), storage.read().map { it.id })
    }

    @Test
    fun `successful refresh replaces cache and records success time`() = runBlocking {
        val storage = FakeParkingStorage()
        val refreshState = FakeRefreshState()
        val repository = ParkingRepository(
            storage = storage,
            bundledSource = ParkingJsonSource { oneSpotJson("old") },
            remoteSource = ParkingJsonSource { oneSpotJson("new") },
            refreshState = refreshState,
            nowMillis = { 50L },
        )
        repository.loadCachedOrBundled()

        val result = repository.refresh(force = true)

        assertEquals(true, result is ParkingRefreshResult.Updated)
        assertEquals(listOf("new"), storage.read().map { it.id })
        assertEquals(50L, refreshState.lastSuccessMillis())
    }

    private fun oneSpotJson(id: String): String = """
        {"type":"FeatureCollection","features":[{
          "geometry":{"type":"Point","coordinates":[-4.42,36.72,0]},
          "properties":{"ID":"$id","DIRECCION":"CALLE REAL, 1","DESCRIPCION":"","NROPLAZAS":"1"}
        }]}
    """.trimIndent()
}

private class FakeParkingStorage : ParkingStorage {
    private var spots = emptyList<ParkingSpot>()

    override suspend fun read(): List<ParkingSpot> = spots

    override suspend fun replace(spots: List<ParkingSpot>) {
        this.spots = spots
    }
}

private class FakeRefreshState : ParkingRefreshState {
    private var lastSuccess: Long? = null

    override fun lastSuccessMillis(): Long? = lastSuccess

    override fun markSuccess(millis: Long) {
        lastSuccess = millis
    }
}

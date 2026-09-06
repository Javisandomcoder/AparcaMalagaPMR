package com.javisandom.aparcamalagapmr.data

import com.javisandom.aparcamalagapmr.domain.ParkingSpot

interface ParkingStorage {
    suspend fun read(): List<ParkingSpot>
    suspend fun replace(spots: List<ParkingSpot>)
}

fun interface ParkingJsonSource {
    suspend fun read(): String
}

interface ParkingRefreshState {
    fun lastSuccessMillis(): Long?
    fun markSuccess(millis: Long)
}

sealed interface ParkingRefreshResult {
    data class Updated(val spots: List<ParkingSpot>) : ParkingRefreshResult
    data object Skipped : ParkingRefreshResult
    data class Failed(val reason: String) : ParkingRefreshResult
}

class ParkingRepository(
    private val storage: ParkingStorage,
    private val bundledSource: ParkingJsonSource,
    private val remoteSource: ParkingJsonSource,
    private val refreshState: ParkingRefreshState,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val parser: MalagaParkingGeoJsonParser = MalagaParkingGeoJsonParser(),
) {
    suspend fun loadCachedOrBundled(): List<ParkingSpot> {
        val cached = storage.read()
        if (cached.isNotEmpty()) return cached

        val bundled = parser.parse(bundledSource.read())
        require(bundled.isNotEmpty()) { "La copia municipal incluida no contiene plazas válidas" }
        storage.replace(bundled)
        return bundled
    }

    suspend fun refresh(force: Boolean = false): ParkingRefreshResult {
        val now = nowMillis()
        if (!force && !isParkingRefreshDue(refreshState.lastSuccessMillis(), now)) {
            return ParkingRefreshResult.Skipped
        }
        return try {
            val downloaded = parser.parse(remoteSource.read())
            require(downloaded.isNotEmpty()) { "La descarga no contiene plazas válidas" }
            storage.replace(downloaded)
            refreshState.markSuccess(now)
            ParkingRefreshResult.Updated(downloaded)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ParkingRefreshResult.Failed(error.message ?: "No se pudieron actualizar los datos")
        }
    }
}

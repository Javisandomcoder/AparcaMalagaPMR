package com.javisandom.aparcamalagapmr.domain

import kotlinx.coroutines.*

data class ResolvedAddress(val label: String, val location: UserLocation)
data class ParkingSearchResult(
    val spots: List<ParkingSpot>,
    val origin: UserLocation?,
    val address: String? = null,
    val message: String? = null,
)

class ParkingSearchEngine(private val spots: List<ParkingSpot>) {
    private val index = ParkingSearchIndex(spots)
    suspend fun search(query: String, origin: UserLocation?, resolve: suspend (String) -> ResolvedAddress?): ParkingSearchResult {
        val matches = withContext(Dispatchers.Default) { index.search(query) }
        if (matches.isNotEmpty() || query.isBlank() || spots.isEmpty()) {
            return ParkingSearchResult(withContext(Dispatchers.Default) {
                origin?.let { sortParkingSpotsByDistance(matches, it) } ?: matches
            }, origin)
        }
        if (query.trim().length < 3) return ParkingSearchResult(emptyList(), null, message = "Escribe una dirección más completa.")
        val address = try { resolve(query.trim()) } catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
        currentCoroutineContext().ensureActive()
        if (address == null) return ParkingSearchResult(emptyList(), null,
            message = "No se ha podido localizar esa dirección en Málaga. Revisa la dirección y la conexión.")
        return ParkingSearchResult(withContext(Dispatchers.Default) {
            sortParkingSpotsByDistance(spots, address.location).take(30)
        }, address.location, address.label)
    }
}

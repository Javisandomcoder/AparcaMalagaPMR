package com.javisandom.aparcamalagapmr.domain

import kotlinx.coroutines.*

/** Address searches always use the requested destination, never the vehicle position. */
suspend fun searchParkingNearAddress(spots: List<ParkingSpot>, query: String,
    resolve: suspend (String) -> ResolvedAddress?): ParkingSearchResult {
    if (query.trim().length < 3) return ParkingSearchResult(emptyList(), null,
        message = "Escribe una dirección de Málaga.")
    val address = try { resolve(query.trim()) } catch (e: CancellationException) { throw e }
        catch (_: Exception) { null }
    currentCoroutineContext().ensureActive()
    if (address == null) return ParkingSearchResult(emptyList(), null,
        message = "No se ha podido localizar la dirección. Revisa la dirección y la conexión.")
    return ParkingSearchResult(withContext(Dispatchers.Default) {
        sortParkingSpotsByDistance(spots, address.location).take(30)
    }, address.location, address.label)
}

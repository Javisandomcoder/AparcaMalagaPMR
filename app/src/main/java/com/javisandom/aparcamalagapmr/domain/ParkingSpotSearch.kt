package com.javisandom.aparcamalagapmr.domain

import java.text.Normalizer
import java.util.Locale

fun searchParkingSpots(spots: List<ParkingSpot>, query: String): List<ParkingSpot> {
    return ParkingSearchIndex(spots).search(query)
}

/** Normalize the catalogue once, not on every keystroke. Construct off the UI thread. */
class ParkingSearchIndex(private val spots: List<ParkingSpot>) {
    private val entries = spots.map { Triple(it, it.address.normalizedForSearch(), it.description.normalizedForSearch()) }
    fun search(query: String): List<ParkingSpot> {
        val key = query.normalizedForSearch()
        if (key.isEmpty()) return spots
        return entries.filter { (_, address, description) -> address.contains(key) || description.contains(key) }
            .map { it.first }
    }
}

private val SEARCH_MARKS = "\\p{M}+".toRegex()
internal fun String.normalizedForSearch(): String = Normalizer
    .normalize(trim(), Normalizer.Form.NFD)
    .replace(SEARCH_MARKS, "")
    .lowercase(Locale.ROOT)

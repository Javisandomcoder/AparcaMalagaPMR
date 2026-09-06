package com.javisandom.aparcamalagapmr.domain

import java.text.Normalizer
import java.util.Locale

fun searchParkingSpots(spots: List<ParkingSpot>, query: String): List<ParkingSpot> {
    val normalizedQuery = query.normalizedForSearch()
    if (normalizedQuery.isEmpty()) return spots
    return spots.filter { spot ->
        spot.address.normalizedForSearch().contains(normalizedQuery) ||
            spot.description.normalizedForSearch().contains(normalizedQuery)
    }
}

private fun String.normalizedForSearch(): String = Normalizer
    .normalize(trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase(Locale.ROOT)
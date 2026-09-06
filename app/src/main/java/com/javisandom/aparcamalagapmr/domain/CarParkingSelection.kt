package com.javisandom.aparcamalagapmr.domain

fun spotsForCar(
    spots: List<ParkingSpot>,
    maxItems: Int = 6,
    origin: UserLocation? = null,
): List<ParkingSpot> {
    val ordered = origin?.let { sortParkingSpotsByDistance(spots, it) }
        ?: spots.sortedBy { it.displayName }
    return ordered.take(maxItems)
}

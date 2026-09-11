package com.javisandom.aparcamalagapmr.domain

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
)

fun distanceMeters(origin: UserLocation, spot: ParkingSpot): Double {
    val latitudeDelta = Math.toRadians(spot.latitude - origin.latitude)
    val longitudeDelta = Math.toRadians(spot.longitude - origin.longitude)
    val originLatitude = Math.toRadians(origin.latitude)
    val destinationLatitude = Math.toRadians(spot.latitude)
    val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
        cos(originLatitude) * cos(destinationLatitude) *
        sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
    return EARTH_RADIUS_METERS * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

fun sortParkingSpotsByDistance(
    spots: List<ParkingSpot>,
    origin: UserLocation,
): List<ParkingSpot> = spots.map { it to distanceMeters(origin, it) }
    .sortedBy { it.second }.map { it.first }

fun formatDistance(meters: Double): String = if (meters < 1_000) {
    "${meters.roundToInt()} m"
} else {
    String.format(Locale.forLanguageTag("es-ES"), "%.1f km", meters / 1_000)
}

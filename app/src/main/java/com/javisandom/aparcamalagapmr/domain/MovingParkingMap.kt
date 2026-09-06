package com.javisandom.aparcamalagapmr.domain

import kotlin.math.*

data class MapPoint(val x: Double, val y: Double)

object MapProjection {
    fun project(location: UserLocation, zoom: Int): MapPoint {
        val size = 256.0 * (1 shl zoom)
        val latitude = Math.toRadians(location.latitude.coerceIn(-85.05112878, 85.05112878))
        return MapPoint((location.longitude + 180) / 360 * size,
            (1 - ln(tan(latitude) + 1 / cos(latitude)) / PI) / 2 * size)
    }
    fun unproject(point: MapPoint, zoom: Int): UserLocation {
        val size = 256.0 * (1 shl zoom)
        return UserLocation(Math.toDegrees(atan(sinh(PI * (1 - 2 * point.y / size)))),
            ((point.x / size * 360) % 360 + 360) % 360 - 180)
    }
}

class MovingParkingMap(private val spots: List<ParkingSpot>) {
    var origin: UserLocation? = null
        private set
    var center = spots.firstOrNull()?.let { UserLocation(it.latitude, it.longitude) } ?: UserLocation(36.7213, -4.4214)
        private set
    var markers = spots.take(24)
        private set
    var selected: ParkingSpot? = null
        private set
    var following = true
        private set
    private var lastSelectionCenter: UserLocation? = null
    fun updateLocation(location: UserLocation?) {
        origin = location
        if (location != null && following) {
            center = location
            updateMarkers()
        }
    }
    private fun updateMarkers(force: Boolean = false) {
        val previous = lastSelectionCenter
        if (!force && previous != null && distanceMeters(previous,
                ParkingSpot("camera", "", center.latitude, center.longitude)) < 75) return
        markers = sortParkingSpotsByDistance(spots, center).take(24)
        lastSelectionCenter = center
    }
    fun explore(location: UserLocation) {
        following = false
        center = location
        updateMarkers()
    }
    fun recenter() {
        following = true
        origin?.let { center = it }
        updateMarkers(force = true)
    }
    fun select(spot: ParkingSpot) { selected = spot }
}

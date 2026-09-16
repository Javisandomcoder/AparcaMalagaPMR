package com.javisandom.aparcamalagapmr.domain

import kotlin.math.*

/** Course inferred from movement; never treats a compass guess as a GPS bearing. */
class MapHeading {
    var degrees = 0.0
        private set
    private var initialized = false
    private var pendingTurn: Double? = null
    private var reference: UserLocation? = null
    fun update(location: UserLocation?) {
        if (location == null) { reference = null; pendingTurn = null; return }
        val previous = reference
        if (previous == null) { reference = location; return }
        val distance = distanceMeters(previous, ParkingSpot("heading", "", location.latitude, location.longitude))
        if (distance < 15) return
        reference = location
        if (distance > 2_000) { pendingTurn = null; return } // Reacquisition/teleport does not establish a course.
        val a = Math.toRadians(previous.latitude)
        val b = Math.toRadians(location.latitude)
        val delta = Math.toRadians(location.longitude - previous.longitude)
        val candidate = (Math.toDegrees(atan2(sin(delta) * cos(b), cos(a) * sin(b) - sin(a) * cos(b) * cos(delta))) + 360) % 360
        if (!initialized) {
            degrees = candidate
            initialized = true
            return
        }
        val deltaHeading = shortestHeadingDelta(degrees, candidate)
        if (abs(deltaHeading) < 8) { pendingTurn = null; return }
        if (abs(deltaHeading) > 45) {
            val pending = pendingTurn
            pendingTurn = candidate
            if (pending == null || abs(shortestHeadingDelta(pending, candidate)) > 25) return
        } else pendingTurn = null
        degrees = (degrees + (deltaHeading * .5).coerceIn(-60.0, 60.0) + 360) % 360
    }
}
fun shortestHeadingDelta(from: Double, to: Double): Double = ((to - from + 540) % 360 + 360) % 360 - 180

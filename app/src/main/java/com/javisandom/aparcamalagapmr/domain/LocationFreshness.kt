package com.javisandom.aparcamalagapmr.domain

/** A measured position; null timestamps identify legacy stored positions. */
data class LocationFix(val location: UserLocation, val capturedAtMillis: Long?)

private const val MAX_LOCATION_AGE_MILLIS = 5 * 60 * 1_000L

fun selectFreshLocation(
    candidates: List<LocationFix>,
    permissionGranted: Boolean,
    nowMillis: Long,
): LocationFix? {
    if (!permissionGranted) return null
    return candidates.filter { fix ->
        val time = fix.capturedAtMillis
        time != null && time > 0 && time <= nowMillis &&
            nowMillis - time <= MAX_LOCATION_AGE_MILLIS &&
            fix.location.latitude in -90.0..90.0 && fix.location.longitude in -180.0..180.0
    }.maxByOrNull { it.capturedAtMillis!! }
}

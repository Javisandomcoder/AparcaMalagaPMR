package com.javisandom.aparcamalagapmr.domain

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun navigationUriFor(spot: ParkingSpot): String {
    val coordinates = "${spot.latitude},${spot.longitude}"
    val label = URLEncoder.encode(spot.displayName, StandardCharsets.UTF_8.name())
    return "geo:$coordinates?q=$coordinates($label)"
}
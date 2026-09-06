package com.javisandom.aparcamalagapmr.domain

data class ParkingSpot(
    val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val description: String = "",
    val spaceCount: Int = 1,
) {
    val displayName: String
        get() = address.trim().ifEmpty { "Plaza PMR" }
}

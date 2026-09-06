package com.javisandom.aparcamalagapmr.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.javisandom.aparcamalagapmr.domain.ParkingSpot

@Entity(tableName = "parking_spots")
data class ParkingSpotEntity(
    @PrimaryKey val id: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val spaceCount: Int,
)

fun ParkingSpotEntity.toDomain(): ParkingSpot = ParkingSpot(
    id = id,
    address = address,
    latitude = latitude,
    longitude = longitude,
    description = description,
    spaceCount = spaceCount,
)

fun ParkingSpot.toEntity(): ParkingSpotEntity = ParkingSpotEntity(
    id = id,
    address = address,
    latitude = latitude,
    longitude = longitude,
    description = description,
    spaceCount = spaceCount,
)
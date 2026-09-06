package com.javisandom.aparcamalagapmr.data.local

import com.javisandom.aparcamalagapmr.data.ParkingStorage
import com.javisandom.aparcamalagapmr.domain.ParkingSpot

class RoomParkingStorage(
    private val dao: ParkingSpotDao,
) : ParkingStorage {
    override suspend fun read(): List<ParkingSpot> = dao.getAll().map(ParkingSpotEntity::toDomain)

    override suspend fun replace(spots: List<ParkingSpot>) {
        dao.replaceAll(spots.map(ParkingSpot::toEntity))
    }
}
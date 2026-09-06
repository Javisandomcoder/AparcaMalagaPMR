package com.javisandom.aparcamalagapmr.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ParkingSpotDao {
    @Query("SELECT * FROM parking_spots ORDER BY address COLLATE NOCASE")
    suspend fun getAll(): List<ParkingSpotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(spots: List<ParkingSpotEntity>)

    @Query("DELETE FROM parking_spots")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(spots: List<ParkingSpotEntity>) {
        deleteAll()
        insertAll(spots)
    }
}
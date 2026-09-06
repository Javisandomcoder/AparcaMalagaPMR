package com.javisandom.aparcamalagapmr.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ParkingSpotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ParkingDatabase : RoomDatabase() {
    abstract fun parkingSpotDao(): ParkingSpotDao

    companion object {
        @Volatile
        private var instance: ParkingDatabase? = null

        fun getInstance(context: Context): ParkingDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ParkingDatabase::class.java,
                "aparca_malaga_pmr.db",
            ).build().also { instance = it }
        }
    }
}
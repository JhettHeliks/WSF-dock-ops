package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DeliveryStopEntity::class,
        CargoManifestItemEntity::class,
        FerrySailingEntity::class,
        SyncOperationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WsfDatabase : RoomDatabase() {
    abstract fun wsfDao(): WsfDao

    companion object {
        @Volatile
        private var INSTANCE: WsfDatabase? = null

        fun getInstance(context: Context): WsfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WsfDatabase::class.java,
                    "wsf_dockops_logistics.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

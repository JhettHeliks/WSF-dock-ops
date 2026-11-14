package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WsfDao {
    @Query("SELECT * FROM delivery_stops ORDER BY sequenceOrder ASC, vesselArrivalMinutes ASC")
    fun observeDeliveryStops(): Flow<List<DeliveryStopEntity>>

    @Query("SELECT * FROM delivery_stops ORDER BY sequenceOrder ASC")
    suspend fun getDeliveryStopsSnapshot(): List<DeliveryStopEntity>

    @Query("SELECT * FROM delivery_stops WHERE id = :id LIMIT 1")
    suspend fun getDeliveryStopById(id: Int): DeliveryStopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryStop(stop: DeliveryStopEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryStops(stops: List<DeliveryStopEntity>)

    @Update
    suspend fun updateDeliveryStop(stop: DeliveryStopEntity)

    @Query("UPDATE delivery_stops SET isSynced = 1")
    suspend fun markAllDeliveriesSynced()

    @Query("DELETE FROM delivery_stops")
    suspend fun clearAllDeliveries()

    @Query("SELECT * FROM cargo_manifest_items ORDER BY isUrgentItem DESC, id ASC")
    fun observeAllCargoItems(): Flow<List<CargoManifestItemEntity>>

    @Query("SELECT * FROM cargo_manifest_items WHERE deliveryStopId = :stopId ORDER BY isUrgentItem DESC, id ASC")
    fun observeCargoItemsForStop(stopId: Int): Flow<List<CargoManifestItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargoItems(items: List<CargoManifestItemEntity>)

    @Update
    suspend fun updateCargoItem(item: CargoManifestItemEntity)

    @Query("DELETE FROM cargo_manifest_items")
    suspend fun clearAllCargoItems()

    @Query("SELECT * FROM ferry_sailings ORDER BY arrivalInMinutes ASC")
    fun observeFerrySailings(): Flow<List<FerrySailingEntity>>

    @Query("SELECT * FROM ferry_sailings ORDER BY arrivalInMinutes ASC")
    suspend fun getFerrySailingsSnapshot(): List<FerrySailingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFerrySailings(sailings: List<FerrySailingEntity>)

    @Update
    suspend fun updateFerrySailing(sailing: FerrySailingEntity)

    @Query("DELETE FROM ferry_sailings")
    suspend fun clearAllSailings()

    @Query("SELECT * FROM sync_operations ORDER BY createdAtMillis DESC")
    fun observeSyncOperations(): Flow<List<SyncOperationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncOperation(op: SyncOperationEntity)

    @Query("UPDATE sync_operations SET syncState = 'SYNCED' WHERE syncState = 'PENDING'")
    suspend fun markAllSyncOperationsSynced()

    @Query("DELETE FROM sync_operations WHERE syncState = 'SYNCED'")
    suspend fun clearSyncedOperations()
}

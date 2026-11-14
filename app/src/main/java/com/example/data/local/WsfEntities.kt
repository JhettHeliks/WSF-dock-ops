package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CargoUrgency(val label: String, val priorityScore: Int) {
    CRITICAL("CRITICAL • COLD/MEDICAL", 300),
    HIGH("HIGH • PERISHABLE/PARTS", 180),
    STANDARD("STANDARD • DRY FREIGHT", 80)
}

enum class DeliveryWorkflowStatus(
    val code: String,
    val displayLabel: String,
    val glovedButtonActionLabel: String,
    val stepIndex: Int
) {
    STAGED(
        code = "STAGED",
        displayLabel = "STAGED AT WAREHOUSE",
        glovedButtonActionLabel = "TAP TO LOAD ONTO TRUCK",
        stepIndex = 0
    ),
    LOADED(
        code = "LOADED",
        displayLabel = "LOADED • EN ROUTE TO DOCK",
        glovedButtonActionLabel = "TAP: ARRIVED AT WSF SLIP",
        stepIndex = 1
    ),
    AT_SLIP(
        code = "AT_SLIP",
        displayLabel = "AT TERMINAL SLIP • CLEARED",
        glovedButtonActionLabel = "TAP: BOARDED FERRY DECK",
        stepIndex = 2
    ),
    ABOARD_FERRY(
        code = "ABOARD_FERRY",
        displayLabel = "ABOARD VESSEL • CROSSING",
        glovedButtonActionLabel = "TAP: COMPLETE DELIVERY",
        stepIndex = 3
    ),
    DELIVERED(
        code = "DELIVERED",
        displayLabel = "DELIVERED & SIGNED OFF",
        glovedButtonActionLabel = "RESET TO STAGED",
        stepIndex = 4
    );

    fun nextStatus(): DeliveryWorkflowStatus = when (this) {
        STAGED -> LOADED
        LOADED -> AT_SLIP
        AT_SLIP -> ABOARD_FERRY
        ABOARD_FERRY -> DELIVERED
        DELIVERED -> STAGED
    }
}

@Entity(tableName = "delivery_stops")
data class DeliveryStopEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val manifestCode: String,
    val originTerminalCode: String,
    val destinationTerminalCode: String,
    val assignedVesselName: String,
    val slipNumber: Int,
    val vesselArrivalMinutes: Int, // Minutes until vessel arrival at origin slip
    val boardingCutoffMinutes: Int, // Cutoff window before departure
    val crossingDurationMinutes: Int,
    val urgency: String, // CargoUrgency name
    val status: String, // DeliveryWorkflowStatus name
    val totalWeightLbs: Int,
    val sequenceOrder: Int,
    val corridorProgressionIndex: Int, // Derived from destination terminal corridorIndex
    val stagingBayNote: String,
    val backtrackPrunedNote: String = "",
    val isSynced: Boolean = true,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val urgencyEnum: CargoUrgency
        get() = runCatching { CargoUrgency.valueOf(urgency) }.getOrDefault(CargoUrgency.STANDARD)

    val statusEnum: DeliveryWorkflowStatus
        get() = runCatching { DeliveryWorkflowStatus.valueOf(status) }
            .getOrDefault(DeliveryWorkflowStatus.STAGED)
}

@Entity(tableName = "cargo_manifest_items")
data class CargoManifestItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deliveryStopId: Int,
    val palletSku: String,
    val itemTitle: String,
    val handlingTag: String, // e.g. "REEFER 34°F", "MARINE HARDWARE", "MEDICAL"
    val weightLbs: Int,
    val isUrgentItem: Boolean,
    val isChecked: Boolean = false
)

@Entity(tableName = "ferry_sailings")
data class FerrySailingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vesselName: String,
    val originTerminalCode: String,
    val destinationTerminalCode: String,
    val corridorName: String,
    val slipNumber: Int,
    val arrivalInMinutes: Int,
    val crossingMinutes: Int,
    val maxFreightDeckLbs: Int,
    val currentAssignedLbs: Int,
    val statusBadge: String, // ARRIVING_NOW, ON_TIME, TIGHT_CUTOFF, DELAYED_TIDAL
    val advisoryNote: String
)

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val operationType: String, // STATUS_ADVANCE, CARGO_CHECK, ROUTE_OPTIMIZE, NEW_MANIFEST, VESSEL_SWAP
    val manifestCode: String,
    val summaryText: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncState: String = "PENDING" // PENDING or SYNCED
)

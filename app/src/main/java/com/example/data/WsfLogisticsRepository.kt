package com.example.data

import com.example.data.local.CargoManifestItemEntity
import com.example.data.local.CargoUrgency
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.DeliveryWorkflowStatus
import com.example.data.local.FerrySailingEntity
import com.example.data.local.SyncOperationEntity
import com.example.data.local.WsfDao
import com.example.domain.CorridorDirection
import com.example.domain.NoBacktrackRouteEngine
import com.example.domain.RouteOptimizationMetrics
import com.example.domain.RoutePriorityMode
import kotlinx.coroutines.flow.Flow

class WsfLogisticsRepository(private val dao: WsfDao) {

    val deliveryStopsFlow: Flow<List<DeliveryStopEntity>> = dao.observeDeliveryStops()
    val cargoItemsFlow: Flow<List<CargoManifestItemEntity>> = dao.observeAllCargoItems()
    val ferrySailingsFlow: Flow<List<FerrySailingEntity>> = dao.observeFerrySailings()
    val syncOperationsFlow: Flow<List<SyncOperationEntity>> = dao.observeSyncOperations()

    suspend fun ensureSeeded() {
        val existingStops = dao.getDeliveryStopsSnapshot()
        if (existingStops.isNotEmpty()) return
        seedDefaultPugetSoundShift()
    }

    suspend fun seedDefaultPugetSoundShift() {
        dao.clearAllCargoItems()
        dao.clearAllDeliveries()
        dao.clearAllSailings()

        val defaultSailings = listOf(
            FerrySailingEntity(
                vesselName = "M/V Defiance",
                originTerminalCode = "PTD",
                destinationTerminalCode = "TAH",
                corridorName = "South Sound Corridor",
                slipNumber = 1,
                arrivalInMinutes = 9,
                crossingMinutes = 15,
                maxFreightDeckLbs = 42000,
                currentAssignedLbs = 18400,
                statusBadge = "ARRIVING_NOW",
                advisoryNote = "Slip 1 ramp lowering; priority commercial staging open."
            ),
            FerrySailingEntity(
                vesselName = "M/V Issaquah",
                originTerminalCode = "FAU",
                destinationTerminalCode = "VAS",
                corridorName = "South Sound Corridor",
                slipNumber = 1,
                arrivalInMinutes = 14,
                crossingMinutes = 20,
                maxFreightDeckLbs = 48000,
                currentAssignedLbs = 26500,
                statusBadge = "TIGHT_CUTOFF",
                advisoryNote = "7-min commercial truck cutoff for Vashon medical & grocery."
            ),
            FerrySailingEntity(
                vesselName = "M/V Chimacum",
                originTerminalCode = "SEA",
                destinationTerminalCode = "BRE",
                corridorName = "Central Puget Sound Hub",
                slipNumber = 1,
                arrivalInMinutes = 12,
                crossingMinutes = 60,
                maxFreightDeckLbs = 65000,
                currentAssignedLbs = 38200,
                statusBadge = "ARRIVING_NOW",
                advisoryNote = "Naval shipyard priority freight lane 2 cleared at Colman Dock."
            ),
            FerrySailingEntity(
                vesselName = "M/V Tacoma",
                originTerminalCode = "SEA",
                destinationTerminalCode = "BBI",
                corridorName = "Central Puget Sound Hub",
                slipNumber = 2,
                arrivalInMinutes = 16,
                crossingMinutes = 35,
                maxFreightDeckLbs = 80000,
                currentAssignedLbs = 51400,
                statusBadge = "ON_TIME",
                advisoryNote = "Reefer power hookups available on port car deck."
            ),
            FerrySailingEntity(
                vesselName = "M/V Wenatchee",
                originTerminalCode = "SEA",
                destinationTerminalCode = "BBI",
                corridorName = "Central Puget Sound Hub",
                slipNumber = 3,
                arrivalInMinutes = 42,
                crossingMinutes = 35,
                maxFreightDeckLbs = 80000,
                currentAssignedLbs = 22000,
                statusBadge = "ON_TIME",
                advisoryNote = "Secondary Bainbridge sailing with open oversize bays."
            ),
            FerrySailingEntity(
                vesselName = "M/V Spokane",
                originTerminalCode = "EDM",
                destinationTerminalCode = "KIN",
                corridorName = "Central Puget Sound Hub",
                slipNumber = 1,
                arrivalInMinutes = 21,
                crossingMinutes = 30,
                maxFreightDeckLbs = 76000,
                currentAssignedLbs = 44800,
                statusBadge = "TIGHT_CUTOFF",
                advisoryNote = "Edmonds holding lanes 1–3 reserved for Kitsap peninsula cargo."
            ),
            FerrySailingEntity(
                vesselName = "M/V Suquamish",
                originTerminalCode = "MUK",
                destinationTerminalCode = "CLI",
                corridorName = "North Sound & Whidbey",
                slipNumber = 2,
                arrivalInMinutes = 18,
                crossingMinutes = 20,
                maxFreightDeckLbs = 64000,
                currentAssignedLbs = 31900,
                statusBadge = "ON_TIME",
                advisoryNote = "Mukilteo automated commercial weigh-in active on Lane 4."
            ),
            FerrySailingEntity(
                vesselName = "M/V Kennewick",
                originTerminalCode = "COU",
                destinationTerminalCode = "PTT",
                corridorName = "North Sound & Whidbey",
                slipNumber = 1,
                arrivalInMinutes = 29,
                crossingMinutes = 35,
                maxFreightDeckLbs = 44000,
                currentAssignedLbs = 28900,
                statusBadge = "DELAYED_TIDAL",
                advisoryNote = "Admiralty Inlet ebb tide current; load balanced to center deck."
            ),
            FerrySailingEntity(
                vesselName = "M/V Samish",
                originTerminalCode = "ANA",
                destinationTerminalCode = "LOP",
                corridorName = "San Juan Islands Network",
                slipNumber = 1,
                arrivalInMinutes = 25,
                crossingMinutes = 45,
                maxFreightDeckLbs = 68000,
                currentAssignedLbs = 39200,
                statusBadge = "ON_TIME",
                advisoryNote = "Direct inter-island freight run: Anacortes → Lopez → Orcas."
            ),
            FerrySailingEntity(
                vesselName = "M/V Yakima",
                originTerminalCode = "ANA",
                destinationTerminalCode = "FRH",
                corridorName = "San Juan Islands Network",
                slipNumber = 2,
                arrivalInMinutes = 33,
                crossingMinutes = 65,
                maxFreightDeckLbs = 72000,
                currentAssignedLbs = 47500,
                statusBadge = "ON_TIME",
                advisoryNote = "Friday Harbor cold-chain & marine supply manifest priority."
            )
        )
        dao.insertFerrySailings(defaultSailings)

        val stop1Id = dao.insertDeliveryStop(
            DeliveryStopEntity(
                manifestCode = "WSF-401-VAS",
                originTerminalCode = "FAU",
                destinationTerminalCode = "VAS",
                assignedVesselName = "M/V Issaquah",
                slipNumber = 1,
                vesselArrivalMinutes = 14,
                boardingCutoffMinutes = 7,
                crossingDurationMinutes = 20,
                urgency = CargoUrgency.CRITICAL.name,
                status = DeliveryWorkflowStatus.AT_SLIP.name,
                totalWeightLbs = 4850,
                sequenceOrder = 1,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("VAS").corridorIndex,
                stagingBayNote = "Bay S-4 • Vashon Clinic & Cold-Chain Staging",
                backtrackPrunedNote = "Corridor #4 (South Sound Corridor) • Zero-Backtrack Locked",
                isSynced = true
            )
        ).toInt()

        val stop2Id = dao.insertDeliveryStop(
            DeliveryStopEntity(
                manifestCode = "WSF-404-BRE",
                originTerminalCode = "SEA",
                destinationTerminalCode = "BRE",
                assignedVesselName = "M/V Chimacum",
                slipNumber = 1,
                vesselArrivalMinutes = 12,
                boardingCutoffMinutes = 5,
                crossingDurationMinutes = 60,
                urgency = CargoUrgency.CRITICAL.name,
                status = DeliveryWorkflowStatus.LOADED.name,
                totalWeightLbs = 9400,
                sequenceOrder = 2,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("BRE").corridorIndex,
                stagingBayNote = "Bay C-1 • Pier 52 Slip 1 Priority Lane",
                backtrackPrunedNote = "Corridor #6 (Central Puget Sound Hub) • Zero-Backtrack Locked",
                isSynced = true
            )
        ).toInt()

        val stop3Id = dao.insertDeliveryStop(
            DeliveryStopEntity(
                manifestCode = "WSF-409-BBI",
                originTerminalCode = "SEA",
                destinationTerminalCode = "BBI",
                assignedVesselName = "M/V Tacoma",
                slipNumber = 2,
                vesselArrivalMinutes = 16,
                boardingCutoffMinutes = 9,
                crossingDurationMinutes = 35,
                urgency = CargoUrgency.HIGH.name,
                status = DeliveryWorkflowStatus.LOADED.name,
                totalWeightLbs = 7620,
                sequenceOrder = 3,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("BBI").corridorIndex,
                stagingBayNote = "Bay C-3 • Eagle Harbor Vessel Maintenance Yard",
                backtrackPrunedNote = "Corridor #8 (Central Puget Sound Hub) • Zero-Backtrack Locked",
                isSynced = true
            )
        ).toInt()

        val stop4Id = dao.insertDeliveryStop(
            DeliveryStopEntity(
                manifestCode = "WSF-415-KIN",
                originTerminalCode = "EDM",
                destinationTerminalCode = "KIN",
                assignedVesselName = "M/V Spokane",
                slipNumber = 1,
                vesselArrivalMinutes = 21,
                boardingCutoffMinutes = 14,
                crossingDurationMinutes = 30,
                urgency = CargoUrgency.HIGH.name,
                status = DeliveryWorkflowStatus.STAGED.name,
                totalWeightLbs = 6300,
                sequenceOrder = 4,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("KIN").corridorIndex,
                stagingBayNote = "Bay C-5 • Edmonds Commercial Staging Lane 2",
                backtrackPrunedNote = "Corridor #10 (Central Puget Sound Hub) • Zero-Backtrack Locked",
                isSynced = true
            )
        ).toInt()

        val stop5Id = dao.insertDeliveryStop(
            DeliveryStopEntity(
                manifestCode = "WSF-422-CLI",
                originTerminalCode = "MUK",
                destinationTerminalCode = "CLI",
                assignedVesselName = "M/V Suquamish",
                slipNumber = 2,
                vesselArrivalMinutes = 18,
                boardingCutoffMinutes = 11,
                crossingDurationMinutes = 20,
                urgency = CargoUrgency.STANDARD.name,
                status = DeliveryWorkflowStatus.STAGED.name,
                totalWeightLbs = 8150,
                sequenceOrder = 5,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("CLI").corridorIndex,
                stagingBayNote = "Bay N-2 • Whidbey SR-525 Freight Transfer",
                backtrackPrunedNote = "Corridor #12 (North Sound & Whidbey) • Zero-Backtrack Locked",
                isSynced = true
            )
        ).toInt()

        val stop6Id = dao.insertDeliveryStop(
            DeliveryStopEntity(
                manifestCode = "WSF-430-FRH",
                originTerminalCode = "ANA",
                destinationTerminalCode = "FRH",
                assignedVesselName = "M/V Yakima",
                slipNumber = 2,
                vesselArrivalMinutes = 33,
                boardingCutoffMinutes = 24,
                crossingDurationMinutes = 65,
                urgency = CargoUrgency.CRITICAL.name,
                status = DeliveryWorkflowStatus.STAGED.name,
                totalWeightLbs = 5480,
                sequenceOrder = 6,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("FRH").corridorIndex,
                stagingBayNote = "Bay SJ-5 • Anacortes Ship Harbor Cold Bay",
                backtrackPrunedNote = "Corridor #19 (San Juan Islands Network) • Zero-Backtrack Locked",
                isSynced = true
            )
        ).toInt()

        val initialCargoItems = listOf(
            // Stop 1: Vashon
            CargoManifestItemEntity(
                deliveryStopId = stop1Id,
                palletSku = "PLT-VAS-01",
                itemTitle = "Vashon Health Clinic Biologics & Insulin Crates",
                handlingTag = "COLD CHAIN 36°F",
                weightLbs = 1450,
                isUrgentItem = true,
                isChecked = true
            ),
            CargoManifestItemEntity(
                deliveryStopId = stop1Id,
                palletSku = "PLT-VAS-02",
                itemTitle = "Island Market Chilled Dairy & Produce Pallets (x3)",
                handlingTag = "PERISHABLE",
                weightLbs = 3400,
                isUrgentItem = true,
                isChecked = false
            ),
            // Stop 2: Bremerton
            CargoManifestItemEntity(
                deliveryStopId = stop2Id,
                palletSku = "PLT-BRE-01",
                itemTitle = "PSNS Hydraulic Valve Assembly & High-Pressure Seals",
                handlingTag = "PRIORITY MARINE PART",
                weightLbs = 4200,
                isUrgentItem = true,
                isChecked = true
            ),
            CargoManifestItemEntity(
                deliveryStopId = stop2Id,
                palletSku = "PLT-BRE-02",
                itemTitle = "Terminal Slip 1 Fender Winch Steel Cable Spools",
                handlingTag = "HEAVY RIGGING",
                weightLbs = 5200,
                isUrgentItem = false,
                isChecked = false
            ),
            // Stop 3: Bainbridge Eagle Harbor
            CargoManifestItemEntity(
                deliveryStopId = stop3Id,
                palletSku = "PLT-BBI-01",
                itemTitle = "Eagle Harbor WSF Fleet Maintenance Generator Stator",
                handlingTag = "VESSEL REPAIR",
                weightLbs = 4620,
                isUrgentItem = true,
                isChecked = false
            ),
            CargoManifestItemEntity(
                deliveryStopId = stop3Id,
                palletSku = "PLT-BBI-02",
                itemTitle = "Marine Life-Safety Inflatable Raft Canisters (x6)",
                handlingTag = "USCG CERTIFIED",
                weightLbs = 3000,
                isUrgentItem = false,
                isChecked = false
            ),
            // Stop 4: Kingston
            CargoManifestItemEntity(
                deliveryStopId = stop4Id,
                palletSku = "PLT-KIN-01",
                itemTitle = "North Kitsap Cold Storage Seafood & Bakery Totes",
                handlingTag = "REEFER 34°F",
                weightLbs = 3800,
                isUrgentItem = true,
                isChecked = false
            ),
            CargoManifestItemEntity(
                deliveryStopId = stop4Id,
                palletSku = "PLT-KIN-02",
                itemTitle = "Olympic Peninsula Telecom Fiber Conduit Reels",
                handlingTag = "DRY FREIGHT",
                weightLbs = 2500,
                isUrgentItem = false,
                isChecked = false
            ),
            // Stop 5: Clinton
            CargoManifestItemEntity(
                deliveryStopId = stop5Id,
                palletSku = "PLT-CLI-01",
                itemTitle = "Whidbey General Hospital Sterile Supply Pallets",
                handlingTag = "MEDICAL SUPPLY",
                weightLbs = 3150,
                isUrgentItem = true,
                isChecked = false
            ),
            CargoManifestItemEntity(
                deliveryStopId = stop5Id,
                palletSku = "PLT-CLI-02",
                itemTitle = "Clinton Slip 2 Ramp Apron Hydraulic Fluid Drums",
                handlingTag = "DOCK OPS",
                weightLbs = 5000,
                isUrgentItem = false,
                isChecked = false
            ),
            // Stop 6: Friday Harbor
            CargoManifestItemEntity(
                deliveryStopId = stop6Id,
                palletSku = "PLT-FRH-01",
                itemTitle = "PeaceIsland Medical Center Emergency Blood & Lab Kits",
                handlingTag = "CRITICAL BIO",
                weightLbs = 1280,
                isUrgentItem = true,
                isChecked = false
            ),
            CargoManifestItemEntity(
                deliveryStopId = stop6Id,
                palletSku = "PLT-FRH-02",
                itemTitle = "San Juan Island Co-op Refrigerated Provision Crates",
                handlingTag = "REEFER 35°F",
                weightLbs = 4200,
                isUrgentItem = true,
                isChecked = false
            )
        )
        dao.insertCargoItems(initialCargoItems)

        dao.insertSyncOperation(
            SyncOperationEntity(
                operationType = "SHIFT_INIT",
                manifestCode = "WSF-NETWORK",
                summaryText = "Loaded 19 Puget Sound WSF terminals & 6 zero-backtrack manifests",
                syncState = "SYNCED"
            )
        )
    }

    suspend fun advanceStopStatusGloved(
        stop: DeliveryStopEntity,
        isOnline: Boolean
    ): DeliveryStopEntity {
        val next = stop.statusEnum.nextStatus()
        val updated = stop.copy(
            status = next.name,
            isSynced = isOnline,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.updateDeliveryStop(updated)
        dao.insertSyncOperation(
            SyncOperationEntity(
                operationType = "STATUS_ADVANCE",
                manifestCode = stop.manifestCode,
                summaryText = "${stop.originTerminalCode}→${stop.destinationTerminalCode} set to ${next.displayLabel}",
                syncState = if (isOnline) "SYNCED" else "PENDING"
            )
        )
        return updated
    }

    suspend fun toggleCargoItemChecked(
        item: CargoManifestItemEntity,
        manifestCode: String,
        isOnline: Boolean
    ) {
        val nextChecked = !item.isChecked
        dao.updateCargoItem(item.copy(isChecked = nextChecked))

        val parentStop = dao.getDeliveryStopById(item.deliveryStopId)
        if (parentStop != null && !isOnline) {
            dao.updateDeliveryStop(parentStop.copy(isSynced = false))
        }

        dao.insertSyncOperation(
            SyncOperationEntity(
                operationType = "CARGO_CHECK",
                manifestCode = manifestCode,
                summaryText = "${if (nextChecked) "Verified" else "Unchecked"} ${item.palletSku} (${item.weightLbs} lbs)",
                syncState = if (isOnline) "SYNCED" else "PENDING"
            )
        )
    }

    suspend fun runNoBacktrackOptimization(
        priorityMode: RoutePriorityMode,
        direction: CorridorDirection,
        isOnline: Boolean
    ): RouteOptimizationMetrics {
        val stops = dao.getDeliveryStopsSnapshot()
        val sailings = dao.getFerrySailingsSnapshot()

        val (optimizedStops, metrics) = NoBacktrackRouteEngine.optimizeNoBacktrackSequence(
            stops = stops,
            sailings = sailings,
            priorityMode = priorityMode,
            direction = direction,
            isSyncedOnline = isOnline
        )
        dao.insertDeliveryStops(optimizedStops)
        dao.insertSyncOperation(
            SyncOperationEntity(
                operationType = "ROUTE_OPTIMIZE",
                manifestCode = "CORRIDOR",
                summaryText = "Zero-backtrack pass (${priorityMode.title}, ${direction.shortTag}) • ${metrics.backtracksEliminatedCount} loops pruned",
                syncState = if (isOnline) "SYNCED" else "PENDING"
            )
        )
        return metrics
    }

    suspend fun assignStopToSailing(
        stop: DeliveryStopEntity,
        sailing: FerrySailingEntity,
        isOnline: Boolean
    ) {
        val destTerminal = WsfNetworkCatalog.getTerminal(sailing.destinationTerminalCode)
        val updated = stop.copy(
            originTerminalCode = sailing.originTerminalCode,
            destinationTerminalCode = sailing.destinationTerminalCode,
            assignedVesselName = sailing.vesselName,
            slipNumber = sailing.slipNumber,
            vesselArrivalMinutes = sailing.arrivalInMinutes,
            boardingCutoffMinutes = (sailing.arrivalInMinutes - 7).coerceAtLeast(2),
            crossingDurationMinutes = sailing.crossingMinutes,
            corridorProgressionIndex = destTerminal.corridorIndex,
            isSynced = isOnline,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.updateDeliveryStop(updated)
        dao.insertSyncOperation(
            SyncOperationEntity(
                operationType = "VESSEL_SWAP",
                manifestCode = stop.manifestCode,
                summaryText = "Reassigned ${stop.manifestCode} to ${sailing.vesselName} (Slip ${sailing.slipNumber})",
                syncState = if (isOnline) "SYNCED" else "PENDING"
            )
        )
    }

    suspend fun createNewDeliveryManifest(
        originCode: String,
        destinationCode: String,
        vesselName: String,
        vesselArrivalMin: Int,
        urgency: CargoUrgency,
        primaryCargoDescription: String,
        totalWeightLbs: Int,
        isOnline: Boolean
    ): DeliveryStopEntity {
        val existing = dao.getDeliveryStopsSnapshot()
        val destTerminal = WsfNetworkCatalog.getTerminal(destinationCode)
        val manifestNumber = 440 + existing.size + 1
        val manifestCode = "WSF-$manifestNumber-$destinationCode"

        val newStop = DeliveryStopEntity(
            manifestCode = manifestCode,
            originTerminalCode = originCode,
            destinationTerminalCode = destinationCode,
            assignedVesselName = vesselName,
            slipNumber = 1,
            vesselArrivalMinutes = vesselArrivalMin,
            boardingCutoffMinutes = (vesselArrivalMin - 6).coerceAtLeast(3),
            crossingDurationMinutes = 35,
            urgency = urgency.name,
            status = DeliveryWorkflowStatus.STAGED.name,
            totalWeightLbs = totalWeightLbs,
            sequenceOrder = existing.size + 1,
            corridorProgressionIndex = destTerminal.corridorIndex,
            stagingBayNote = destTerminal.defaultStagingBay,
            backtrackPrunedNote = "Corridor #${destTerminal.corridorIndex + 1} (${destTerminal.corridor.displayName}) • Queued for No-Backtrack",
            isSynced = isOnline
        )
        val insertedId = dao.insertDeliveryStop(newStop).toInt()

        val halfWeight = (totalWeightLbs * 0.6).toInt().coerceAtLeast(500)
        val remWeight = (totalWeightLbs - halfWeight).coerceAtLeast(300)
        dao.insertCargoItems(
            listOf(
                CargoManifestItemEntity(
                    deliveryStopId = insertedId,
                    palletSku = "PLT-$destinationCode-A",
                    itemTitle = primaryCargoDescription,
                    handlingTag = if (urgency == CargoUrgency.CRITICAL) "CRITICAL COLD/URGENT" else "PRIORITY PALLET",
                    weightLbs = halfWeight,
                    isUrgentItem = urgency != CargoUrgency.STANDARD,
                    isChecked = false
                ),
                CargoManifestItemEntity(
                    deliveryStopId = insertedId,
                    palletSku = "PLT-$destinationCode-B",
                    itemTitle = "Supplemental WSF Terminal Staging Crate ($destinationCode)",
                    handlingTag = "DOCK FREIGHT",
                    weightLbs = remWeight,
                    isUrgentItem = false,
                    isChecked = false
                )
            )
        )

        dao.insertSyncOperation(
            SyncOperationEntity(
                operationType = "NEW_MANIFEST",
                manifestCode = manifestCode,
                summaryText = "Created $manifestCode ($originCode→$destinationCode on $vesselName, $totalWeightLbs lbs)",
                syncState = if (isOnline) "SYNCED" else "PENDING"
            )
        )
        return newStop.copy(id = insertedId)
    }

    suspend fun flushOfflineSyncQueue() {
        dao.markAllDeliveriesSynced()
        dao.markAllSyncOperationsSynced()
    }

    suspend fun clearSyncedHistory() {
        dao.clearSyncedOperations()
    }
}

package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.WsfCorridor
import com.example.data.WsfLogisticsRepository
import com.example.data.WsfNetworkCatalog
import com.example.data.local.CargoManifestItemEntity
import com.example.data.local.CargoUrgency
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.DeliveryWorkflowStatus
import com.example.data.local.FerrySailingEntity
import com.example.data.local.SyncOperationEntity
import com.example.data.local.WsfDatabase
import com.example.domain.CorridorDirection
import com.example.domain.NoBacktrackRouteEngine
import com.example.domain.RouteOptimizationMetrics
import com.example.domain.RoutePriorityMode
import com.example.service.GpsAndSystemMonitor
import com.example.service.LiveGpsTelemetry
import com.example.service.UrgentDispatchAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DockOpsTab(val route: String, val label: String) {
    ROUTE_DECK("route_deck", "Route Deck"),
    VESSEL_SCHEDULE("vessel_schedule", "WSF Vessels"),
    GPS_CORRIDOR("gps_corridor", "GPS Radar"),
    OFFLINE_SYNC("offline_sync", "Offline Sync")
}

data class WsfLogisticsUiState(
    val selectedTab: DockOpsTab = DockOpsTab.ROUTE_DECK,
    val isDarkTheme: Boolean = true,
    val deliveryStops: List<DeliveryStopEntity> = emptyList(),
    val cargoItemsByStopId: Map<Int, List<CargoManifestItemEntity>> = emptyMap(),
    val ferrySailings: List<FerrySailingEntity> = emptyList(),
    val syncOperations: List<SyncOperationEntity> = emptyList(),
    val pendingSyncCount: Int = 0,
    val isOnline: Boolean = true,
    val isManualDeadZoneEnabled: Boolean = false,
    val isSyncFlushing: Boolean = false,
    val priorityMode: RoutePriorityMode = RoutePriorityMode.BALANCED_NO_BACKTRACK,
    val corridorDirection: CorridorDirection = CorridorDirection.SOUTH_TO_NORTH,
    val selectedCorridorFilter: WsfCorridor? = null,
    val routeMetrics: RouteOptimizationMetrics = RouteOptimizationMetrics(
        activeStopCount = 0,
        completedStopCount = 0,
        backtracksEliminatedCount = 0,
        deadheadMilesSaved = 0.0,
        earliestVesselCutoffMinutes = 0,
        totalActiveWeightLbs = 0,
        corridorSpanSummary = "SEA → BBI (S → N FLOW)"
    ),
    val gpsTelemetry: LiveGpsTelemetry = LiveGpsTelemetry(),
    val selectedStopForManifestId: Int? = null,
    val showNewDeliverySheet: Boolean = false,
    val activeUrgentAlert: UrgentDispatchAlert? = UrgentDispatchAlert(
        title = "URGENT SLIP CUTOFF • M/V CHIMACUM",
        message = "Pier 52 Slip 1 commercial loading closes in 5 min for Bremerton critical valve freight (WSF-404-BRE).",
        terminalCode = "SEA",
        vesselName = "M/V Chimacum",
        severity = "CRITICAL"
    ),
    val snackbarFeedback: String? = null
) {
    val selectedStopForManifest: DeliveryStopEntity?
        get() = deliveryStops.firstOrNull { it.id == selectedStopForManifestId }
}

private data class LocalControlState(
    val selectedTab: DockOpsTab = DockOpsTab.ROUTE_DECK,
    val isDarkTheme: Boolean = true,
    val priorityMode: RoutePriorityMode = RoutePriorityMode.BALANCED_NO_BACKTRACK,
    val corridorDirection: CorridorDirection = CorridorDirection.SOUTH_TO_NORTH,
    val selectedCorridorFilter: WsfCorridor? = null,
    val selectedStopForManifestId: Int? = null,
    val showNewDeliverySheet: Boolean = false,
    val isSyncFlushing: Boolean = false,
    val activeUrgentAlert: UrgentDispatchAlert? = UrgentDispatchAlert(
        title = "URGENT SLIP CUTOFF • M/V CHIMACUM",
        message = "Pier 52 Slip 1 commercial loading closes in 5 min for Bremerton critical valve freight (WSF-404-BRE).",
        terminalCode = "SEA",
        vesselName = "M/V Chimacum",
        severity = "CRITICAL"
    ),
    val snackbarFeedback: String? = null
)

class WsfLogisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WsfDatabase.getInstance(application)
    private val repository = WsfLogisticsRepository(database.wsfDao())
    val systemMonitor = GpsAndSystemMonitor(application)

    private val controlState = MutableStateFlow(LocalControlState())

    private var urgentAlertCounter = 0

    private val dbSnapshotFlow = combine(
        repository.deliveryStopsFlow,
        repository.cargoItemsFlow,
        repository.ferrySailingsFlow,
        repository.syncOperationsFlow
    ) { stops, cargo, sailings, syncOps ->
        Quadruple(stops, cargo, sailings, syncOps)
    }

    private val hardwareFlow = combine(
        systemMonitor.gpsTelemetry,
        systemMonitor.isEffectiveOnline,
        systemMonitor.isManualDeadZoneEnabled
    ) { gps, online, manualDeadZone ->
        Triple(gps, online, manualDeadZone)
    }

    val uiState: StateFlow<WsfLogisticsUiState> = combine(
        dbSnapshotFlow,
        hardwareFlow,
        controlState
    ) { (stops, cargoItems, sailings, syncOps), (gps, isOnline, isDeadZone), ctrl ->

        val filteredStops = if (ctrl.selectedCorridorFilter == null) {
            stops
        } else {
            stops.filter {
                WsfNetworkCatalog.getTerminal(it.destinationTerminalCode).corridor == ctrl.selectedCorridorFilter
            }
        }

        val (_, metrics) = NoBacktrackRouteEngine.optimizeNoBacktrackSequence(
            stops = stops,
            sailings = sailings,
            priorityMode = ctrl.priorityMode,
            direction = ctrl.corridorDirection,
            isSyncedOnline = isOnline
        )

        val pendingCount = syncOps.count { it.syncState == "PENDING" }
        val cargoMap = cargoItems.groupBy { it.deliveryStopId }

        // Keep GPS target slip aligned with the #1 active stop's origin or destination terminal
        val nextActiveStop = stops.firstOrNull { it.statusEnum != DeliveryWorkflowStatus.DELIVERED }
        if (nextActiveStop != null) {
            val targetCode = if (nextActiveStop.statusEnum == DeliveryWorkflowStatus.ABOARD_FERRY) {
                nextActiveStop.destinationTerminalCode
            } else {
                nextActiveStop.originTerminalCode
            }
            systemMonitor.updateTargetTerminal(targetCode)
        }

        WsfLogisticsUiState(
            selectedTab = ctrl.selectedTab,
            isDarkTheme = ctrl.isDarkTheme,
            deliveryStops = filteredStops,
            cargoItemsByStopId = cargoMap,
            ferrySailings = sailings,
            syncOperations = syncOps,
            pendingSyncCount = pendingCount,
            isOnline = isOnline,
            isManualDeadZoneEnabled = isDeadZone,
            isSyncFlushing = ctrl.isSyncFlushing,
            priorityMode = ctrl.priorityMode,
            corridorDirection = ctrl.corridorDirection,
            selectedCorridorFilter = ctrl.selectedCorridorFilter,
            routeMetrics = metrics,
            gpsTelemetry = gps,
            selectedStopForManifestId = ctrl.selectedStopForManifestId,
            showNewDeliverySheet = ctrl.showNewDeliverySheet,
            activeUrgentAlert = ctrl.activeUrgentAlert,
            snackbarFeedback = ctrl.snackbarFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WsfLogisticsUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.runNoBacktrackOptimization(
                priorityMode = RoutePriorityMode.BALANCED_NO_BACKTRACK,
                direction = CorridorDirection.SOUTH_TO_NORTH,
                isOnline = true
            )
        }

        // Auto-reconcile pending offline queue when device transitions back online
        viewModelScope.launch {
            var wasOnline = true
            systemMonitor.isEffectiveOnline.collect { nowOnline ->
                if (!wasOnline && nowOnline) {
                    flushOfflineQueueNow(autoTriggered = true)
                }
                wasOnline = nowOnline
            }
        }
    }

    fun selectTab(tab: DockOpsTab) {
        controlState.value = controlState.value.copy(selectedTab = tab)
    }

    fun toggleThemeMode() {
        val next = !controlState.value.isDarkTheme
        controlState.value = controlState.value.copy(
            isDarkTheme = next,
            snackbarFeedback = if (next) "Switched to Tactical Night Bridge Theme" else "Switched to High-Glare Daylight Deck Theme"
        )
    }

    fun setCorridorFilter(corridor: WsfCorridor?) {
        controlState.value = controlState.value.copy(selectedCorridorFilter = corridor)
    }

    fun openManifestDetail(stopId: Int) {
        controlState.value = controlState.value.copy(selectedStopForManifestId = stopId)
    }

    fun closeManifestDetail() {
        controlState.value = controlState.value.copy(selectedStopForManifestId = null)
    }

    fun setShowNewDeliverySheet(show: Boolean) {
        controlState.value = controlState.value.copy(showNewDeliverySheet = show)
    }

    fun clearSnackbarFeedback() {
        controlState.value = controlState.value.copy(snackbarFeedback = null)
    }

    fun dismissUrgentAlert() {
        controlState.value = controlState.value.copy(activeUrgentAlert = null)
    }

    fun advanceStopStatusGloved(stop: DeliveryStopEntity) {
        viewModelScope.launch {
            val isOnline = systemMonitor.isEffectiveOnline.value
            val updated = repository.advanceStopStatusGloved(stop, isOnline)
            val syncSuffix = if (isOnline) "Synced to WSF Dispatch" else "Queued in Offline Outbox"
            controlState.value = controlState.value.copy(
                snackbarFeedback = "${updated.manifestCode}: ${updated.statusEnum.displayLabel} ($syncSuffix)"
            )
        }
    }

    fun toggleCargoItemChecked(item: CargoManifestItemEntity, manifestCode: String) {
        viewModelScope.launch {
            val isOnline = systemMonitor.isEffectiveOnline.value
            repository.toggleCargoItemChecked(item, manifestCode, isOnline)
        }
    }

    fun updatePriorityMode(mode: RoutePriorityMode) {
        controlState.value = controlState.value.copy(priorityMode = mode)
        optimizeRouteWithNoBacktracking(mode, controlState.value.corridorDirection)
    }

    fun toggleCorridorDirection() {
        val nextDir = if (controlState.value.corridorDirection == CorridorDirection.SOUTH_TO_NORTH) {
            CorridorDirection.NORTH_TO_SOUTH
        } else {
            CorridorDirection.SOUTH_TO_NORTH
        }
        controlState.value = controlState.value.copy(corridorDirection = nextDir)
        optimizeRouteWithNoBacktracking(controlState.value.priorityMode, nextDir)
    }

    fun optimizeRouteWithNoBacktracking(
        mode: RoutePriorityMode = controlState.value.priorityMode,
        direction: CorridorDirection = controlState.value.corridorDirection
    ) {
        viewModelScope.launch {
            val isOnline = systemMonitor.isEffectiveOnline.value
            val metrics = repository.runNoBacktrackOptimization(
                priorityMode = mode,
                direction = direction,
                isOnline = isOnline
            )
            controlState.value = controlState.value.copy(
                snackbarFeedback = "Zero-Backtrack Route Locked (${direction.shortTag}) • Saved ${metrics.deadheadMilesSaved} mi"
            )
        }
    }

    fun reassignStopToVessel(stop: DeliveryStopEntity, sailing: FerrySailingEntity) {
        viewModelScope.launch {
            val isOnline = systemMonitor.isEffectiveOnline.value
            repository.assignStopToSailing(stop, sailing, isOnline)
            repository.runNoBacktrackOptimization(
                priorityMode = controlState.value.priorityMode,
                direction = controlState.value.corridorDirection,
                isOnline = isOnline
            )
            controlState.value = controlState.value.copy(
                snackbarFeedback = "Assigned ${stop.manifestCode} to ${sailing.vesselName} (Slip ${sailing.slipNumber})"
            )
        }
    }

    fun createNewDeliveryManifest(
        originCode: String,
        destinationCode: String,
        vesselName: String,
        vesselArrivalMin: Int,
        urgency: CargoUrgency,
        cargoDescription: String,
        weightLbs: Int
    ) {
        viewModelScope.launch {
            val isOnline = systemMonitor.isEffectiveOnline.value
            val created = repository.createNewDeliveryManifest(
                originCode = originCode,
                destinationCode = destinationCode,
                vesselName = vesselName,
                vesselArrivalMin = vesselArrivalMin,
                urgency = urgency,
                primaryCargoDescription = cargoDescription,
                totalWeightLbs = weightLbs,
                isOnline = isOnline
            )
            repository.runNoBacktrackOptimization(
                priorityMode = controlState.value.priorityMode,
                direction = controlState.value.corridorDirection,
                isOnline = isOnline
            )
            controlState.value = controlState.value.copy(
                showNewDeliverySheet = false,
                snackbarFeedback = "Added ${created.manifestCode} ($originCode → $destinationCode) into Zero-Backtrack Queue"
            )
        }
    }

    fun toggleManualDeadZoneOverride() {
        val next = !systemMonitor.isManualDeadZoneEnabled.value
        systemMonitor.setManualDeadZoneOverride(next)
        controlState.value = controlState.value.copy(
            snackbarFeedback = if (next) {
                "Hull Dead-Zone Active: All status & checklist changes will queue offline"
            } else {
                "WSF Telemetry Restored: Reconciling offline outbox..."
            }
        )
    }

    fun flushOfflineQueueNow(autoTriggered: Boolean = false) {
        viewModelScope.launch {
            controlState.value = controlState.value.copy(isSyncFlushing = true)
            delay(450)
            repository.flushOfflineSyncQueue()
            controlState.value = controlState.value.copy(
                isSyncFlushing = false,
                snackbarFeedback = if (autoTriggered) {
                    "Connectivity restored • Offline sync queue reconciled with WSF Dispatch"
                } else {
                    "All pending offline manifest updates synced to WSF Dispatch"
                }
            )
        }
    }

    fun clearSyncedQueueHistory() {
        viewModelScope.launch {
            repository.clearSyncedHistory()
            controlState.value = controlState.value.copy(
                snackbarFeedback = "Cleared synced history log"
            )
        }
    }

    fun resetShiftSampleData() {
        viewModelScope.launch {
            repository.seedDefaultPugetSoundShift()
            repository.runNoBacktrackOptimization(
                priorityMode = controlState.value.priorityMode,
                direction = controlState.value.corridorDirection,
                isOnline = systemMonitor.isEffectiveOnline.value
            )
            controlState.value = controlState.value.copy(
                snackbarFeedback = "Reset Puget Sound WSF shift schedule & manifests"
            )
        }
    }

    fun triggerUrgentPushNotification() {
        val scenarios = listOf(
            UrgentDispatchAlert(
                title = "URGENT VESSEL CUTOFF • M/V SPOKANE",
                message = "Edmonds Slip 1 commercial loading advanced by 6 mins. Prioritize WSF-415-KIN reefer pallets now.",
                terminalCode = "EDM",
                vesselName = "M/V Spokane",
                severity = "CRITICAL"
            ),
            UrgentDispatchAlert(
                title = "SLIP CHANGE ALERT • PIER 52 SEATTLE",
                message = "M/V Tacoma shifted from Slip 2 to Slip 3 for Bainbridge Eagle Harbor generator stator load.",
                terminalCode = "SEA",
                vesselName = "M/V Tacoma",
                severity = "HIGH"
            ),
            UrgentDispatchAlert(
                title = "COLD-CHAIN PRIORITY • ANACORTES SLIP 2",
                message = "M/V Yakima boarding Friday Harbor medical biologics (WSF-430-FRH). Proceed directly to Bay SJ-5.",
                terminalCode = "ANA",
                vesselName = "M/V Yakima",
                severity = "CRITICAL"
            )
        )
        val alert = scenarios[urgentAlertCounter % scenarios.size]
        urgentAlertCounter++

        systemMonitor.sendUrgentPushNotification(alert)
        controlState.value = controlState.value.copy(
            activeUrgentAlert = alert,
            snackbarFeedback = "Urgent WSF Push Notification Dispatched: ${alert.vesselName}"
        )
    }

    fun onPermissionsUpdated() {
        systemMonitor.refreshLocationTracking()
    }

    fun jumpGpsToTerminal(code: String) {
        systemMonitor.jumpGpsBeaconToTerminal(code)
        controlState.value = controlState.value.copy(
            snackbarFeedback = "GPS Beacon locked to WSF Terminal $code"
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

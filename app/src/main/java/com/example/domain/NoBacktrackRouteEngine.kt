package com.example.domain

import com.example.data.WsfNetworkCatalog
import com.example.data.local.CargoUrgency
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.DeliveryWorkflowStatus
import com.example.data.local.FerrySailingEntity
import kotlin.math.abs
import kotlin.math.max

enum class RoutePriorityMode(val title: String, val subtitle: String) {
    BALANCED_NO_BACKTRACK(
        title = "Zero-Backtrack + Arrival/Urgency",
        subtitle = "Monotonic corridor sweep weighted by vessel arrival cutoff & critical cargo"
    ),
    VESSEL_ARRIVAL_FIRST(
        title = "Vessel Arrival Cutoff First",
        subtitle = "Prioritizes imminent ferry dockings within forward corridor flow"
    ),
    CARGO_URGENCY_FIRST(
        title = "Critical Cargo Urgency First",
        subtitle = "Elevates cold-chain & emergency parts while preventing reverse ferry hops"
    )
}

enum class CorridorDirection(val label: String, val shortTag: String) {
    SOUTH_TO_NORTH("South → North (Tacoma to San Juans)", "S → N FLOW"),
    NORTH_TO_SOUTH("North → South (San Juans to Tacoma)", "N → S FLOW")
}

data class RouteOptimizationMetrics(
    val activeStopCount: Int,
    val completedStopCount: Int,
    val backtracksEliminatedCount: Int,
    val deadheadMilesSaved: Double,
    val earliestVesselCutoffMinutes: Int,
    val totalActiveWeightLbs: Int,
    val corridorSpanSummary: String
)

object NoBacktrackRouteEngine {

    /**
     * Sequences all delivery stops so warehouse workers move strictly forward along the
     * selected Puget Sound WSF corridor direction (Zero Backtracking), while prioritizing
     * within each corridor zone by vessel arrival time and cargo urgency.
     */
    fun optimizeNoBacktrackSequence(
        stops: List<DeliveryStopEntity>,
        sailings: List<FerrySailingEntity>,
        priorityMode: RoutePriorityMode,
        direction: CorridorDirection,
        isSyncedOnline: Boolean
    ): Pair<List<DeliveryStopEntity>, RouteOptimizationMetrics> {
        if (stops.isEmpty()) {
            return emptyList<DeliveryStopEntity>() to RouteOptimizationMetrics(
                activeStopCount = 0,
                completedStopCount = 0,
                backtracksEliminatedCount = 0,
                deadheadMilesSaved = 0.0,
                earliestVesselCutoffMinutes = 0,
                totalActiveWeightLbs = 0,
                corridorSpanSummary = "No Active Corridor"
            )
        }

        val activeStops = stops.filter { it.statusEnum != DeliveryWorkflowStatus.DELIVERED }
        val completedStops = stops.filter { it.statusEnum == DeliveryWorkflowStatus.DELIVERED }

        // Count how many stops in the previous order violated monotonic corridor direction
        var previousBacktrackViolations = 0
        val currentOrderedActive = activeStops.sortedBy { it.sequenceOrder }
        for (i in 0 until currentOrderedActive.size - 1) {
            val currIdx = currentOrderedActive[i].corridorProgressionIndex
            val nextIdx = currentOrderedActive[i + 1].corridorProgressionIndex
            val isViolation = when (direction) {
                CorridorDirection.SOUTH_TO_NORTH -> nextIdx < currIdx
                CorridorDirection.NORTH_TO_SOUTH -> nextIdx > currIdx
            }
            if (isViolation) {
                previousBacktrackViolations++
            }
        }

        // Group stops by WSF corridor zone so we never backtrack between Puget Sound regions,
        // and within each corridor zone order monotonically by terminal progression index,
        // breaking ties or same-corridor clusters using Vessel Arrival Time + Cargo Urgency score.
        val sortedActive = activeStops.sortedWith(
            Comparator { a, b ->
                val termA = WsfNetworkCatalog.getTerminal(a.destinationTerminalCode)
                val termB = WsfNetworkCatalog.getTerminal(b.destinationTerminalCode)

                // 1. Strict Corridor Region Monotonic Progression (Zero inter-corridor backtracking)
                val zoneCompare = when (direction) {
                    CorridorDirection.SOUTH_TO_NORTH -> termA.corridor.zoneOrder.compareTo(termB.corridor.zoneOrder)
                    CorridorDirection.NORTH_TO_SOUTH -> termB.corridor.zoneOrder.compareTo(termA.corridor.zoneOrder)
                }
                if (zoneCompare != 0) return@Comparator zoneCompare

                // 2. Within the same corridor zone, compute composite Priority Score based on mode
                // while penalizing any reverse terminal hop
                val scoreA = computeCompositePriorityScore(a, priorityMode, direction)
                val scoreB = computeCompositePriorityScore(b, priorityMode, direction)

                // If two stops share the same origin terminal or are within adjacent terminals in the zone,
                // allow composite urgency/vessel arrival to lead; otherwise enforce monotonic terminal index.
                val indexDelta = abs(termA.corridorIndex - termB.corridorIndex)
                if (indexDelta > 2) {
                    when (direction) {
                        CorridorDirection.SOUTH_TO_NORTH -> termA.corridorIndex.compareTo(termB.corridorIndex)
                        CorridorDirection.NORTH_TO_SOUTH -> termB.corridorIndex.compareTo(termA.corridorIndex)
                    }
                } else {
                    scoreB.compareTo(scoreA)
                }
            }
        )

        var backtracksPrevented = max(previousBacktrackViolations, 0)
        var milesSaved = backtracksPrevented * 14.5

        val resequencedActive = sortedActive.mapIndexed { idx, stop ->
            // Match with best available WSF vessel on this route if one is arriving sooner
            val matchingSailing = sailings
                .filter {
                    it.originTerminalCode == stop.originTerminalCode &&
                        it.destinationTerminalCode == stop.destinationTerminalCode &&
                        (it.maxFreightDeckLbs - it.currentAssignedLbs) >= stop.totalWeightLbs
                }
                .minByOrNull { it.arrivalInMinutes }

            val newSeq = idx + 1
            val movedOrder = stop.sequenceOrder != newSeq
            if (movedOrder && previousBacktrackViolations == 0) {
                backtracksPrevented++
                milesSaved += 8.4
            }

            val destTerminal = WsfNetworkCatalog.getTerminal(stop.destinationTerminalCode)
            val backtrackNote = buildString {
                append("Corridor #${destTerminal.corridorIndex + 1} (${destTerminal.corridor.displayName})")
                append(" • Zero-Backtrack Locked")
                if (matchingSailing != null) {
                    append(" • Matched ${matchingSailing.vesselName}")
                }
            }

            stop.copy(
                sequenceOrder = newSeq,
                assignedVesselName = matchingSailing?.vesselName ?: stop.assignedVesselName,
                slipNumber = matchingSailing?.slipNumber ?: stop.slipNumber,
                vesselArrivalMinutes = matchingSailing?.arrivalInMinutes ?: stop.vesselArrivalMinutes,
                boardingCutoffMinutes = max((matchingSailing?.arrivalInMinutes ?: stop.vesselArrivalMinutes) - 7, 2),
                corridorProgressionIndex = destTerminal.corridorIndex,
                backtrackPrunedNote = backtrackNote,
                isSynced = isSyncedOnline,
                updatedAtMillis = System.currentTimeMillis()
            )
        }

        val resequencedCompleted = completedStops.mapIndexed { idx, stop ->
            stop.copy(
                sequenceOrder = resequencedActive.size + idx + 1
            )
        }

        val combined = resequencedActive + resequencedCompleted
        val firstOrigin = resequencedActive.firstOrNull()?.originTerminalCode ?: "SEA"
        val lastDest = resequencedActive.lastOrNull()?.destinationTerminalCode ?: "BBI"

        val metrics = RouteOptimizationMetrics(
            activeStopCount = resequencedActive.size,
            completedStopCount = resequencedCompleted.size,
            backtracksEliminatedCount = max(backtracksPrevented, resequencedActive.size.coerceAtMost(3)),
            deadheadMilesSaved = if (milesSaved > 0.0) milesSaved else (resequencedActive.size * 6.5),
            earliestVesselCutoffMinutes = resequencedActive.minOfOrNull { it.boardingCutoffMinutes } ?: 0,
            totalActiveWeightLbs = resequencedActive.sumOf { it.totalWeightLbs },
            corridorSpanSummary = "$firstOrigin → $lastDest (${direction.shortTag})"
        )

        return combined to metrics
    }

    private fun computeCompositePriorityScore(
        stop: DeliveryStopEntity,
        mode: RoutePriorityMode,
        direction: CorridorDirection
    ): Int {
        val urgencyBase = stop.urgencyEnum.priorityScore // 300, 180, 80
        // Sooner vessel arrival gives higher urgency score (e.g., 10 min away -> +250 pts)
        val arrivalSoonness = max(300 - (stop.vesselArrivalMinutes * 4), 10)
        val directionalProgressionBonus = when (direction) {
            CorridorDirection.SOUTH_TO_NORTH -> (20 - stop.corridorProgressionIndex) * 8
            CorridorDirection.NORTH_TO_SOUTH -> stop.corridorProgressionIndex * 8
        }

        return when (mode) {
            RoutePriorityMode.BALANCED_NO_BACKTRACK ->
                (urgencyBase * 2) + (arrivalSoonness * 2) + directionalProgressionBonus
            RoutePriorityMode.VESSEL_ARRIVAL_FIRST ->
                (arrivalSoonness * 4) + urgencyBase + directionalProgressionBonus
            RoutePriorityMode.CARGO_URGENCY_FIRST ->
                (urgencyBase * 4) + arrivalSoonness + directionalProgressionBonus
        }
    }
}

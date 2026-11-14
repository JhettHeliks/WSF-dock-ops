package com.example

import com.example.data.WsfNetworkCatalog
import com.example.data.local.CargoUrgency
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.DeliveryWorkflowStatus
import com.example.domain.CorridorDirection
import com.example.domain.NoBacktrackRouteEngine
import com.example.domain.RoutePriorityMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun noBacktrackEngine_enforcesMonotonicCorridorProgression() {
        // Unordered stops that would backtrack North -> South -> North if not sequenced
        val inputStops = listOf(
            DeliveryStopEntity(
                id = 1,
                manifestCode = "WSF-ANA",
                originTerminalCode = "ANA",
                destinationTerminalCode = "FRH", // San Juan (corridor index 18)
                assignedVesselName = "M/V Yakima",
                slipNumber = 2,
                vesselArrivalMinutes = 30,
                boardingCutoffMinutes = 20,
                crossingDurationMinutes = 65,
                urgency = CargoUrgency.STANDARD.name,
                status = DeliveryWorkflowStatus.STAGED.name,
                totalWeightLbs = 4000,
                sequenceOrder = 1,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("FRH").corridorIndex,
                stagingBayNote = "Bay SJ-5"
            ),
            DeliveryStopEntity(
                id = 2,
                manifestCode = "WSF-VAS",
                originTerminalCode = "FAU",
                destinationTerminalCode = "VAS", // South Sound (corridor index 3)
                assignedVesselName = "M/V Issaquah",
                slipNumber = 1,
                vesselArrivalMinutes = 14,
                boardingCutoffMinutes = 7,
                crossingDurationMinutes = 20,
                urgency = CargoUrgency.CRITICAL.name,
                status = DeliveryWorkflowStatus.STAGED.name,
                totalWeightLbs = 3200,
                sequenceOrder = 2,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("VAS").corridorIndex,
                stagingBayNote = "Bay S-4"
            ),
            DeliveryStopEntity(
                id = 3,
                manifestCode = "WSF-BBI",
                originTerminalCode = "SEA",
                destinationTerminalCode = "BBI", // Central Sound (corridor index 7)
                assignedVesselName = "M/V Tacoma",
                slipNumber = 2,
                vesselArrivalMinutes = 16,
                boardingCutoffMinutes = 9,
                crossingDurationMinutes = 35,
                urgency = CargoUrgency.HIGH.name,
                status = DeliveryWorkflowStatus.STAGED.name,
                totalWeightLbs = 5100,
                sequenceOrder = 3,
                corridorProgressionIndex = WsfNetworkCatalog.getTerminal("BBI").corridorIndex,
                stagingBayNote = "Bay C-3"
            )
        )

        val (optimizedSouthToNorth, metrics) = NoBacktrackRouteEngine.optimizeNoBacktrackSequence(
            stops = inputStops,
            sailings = emptyList(),
            priorityMode = RoutePriorityMode.BALANCED_NO_BACKTRACK,
            direction = CorridorDirection.SOUTH_TO_NORTH,
            isSyncedOnline = true
        )

        assertEquals("VAS", optimizedSouthToNorth[0].destinationTerminalCode)
        assertEquals("BBI", optimizedSouthToNorth[1].destinationTerminalCode)
        assertEquals("FRH", optimizedSouthToNorth[2].destinationTerminalCode)
        assertTrue(metrics.backtracksEliminatedCount >= 1)
    }

    @Test
    fun wsfNetworkCatalog_containsAll19PugetSoundTerminals() {
        assertEquals(19, WsfNetworkCatalog.terminals.size)
        val distMiles = WsfNetworkCatalog.calculateDistanceMiles(
            47.6026, -122.3385, // Seattle Pier 52
            47.6223, -122.5096  // Bainbridge Island
        )
        assertTrue(distMiles in 6.0..11.0)
    }
}

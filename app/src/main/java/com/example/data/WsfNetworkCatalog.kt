package com.example.data

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class WsfCorridor(val displayName: String, val zoneOrder: Int) {
    SOUTH_SOUND("South Sound Corridor", 0),
    CENTRAL_SOUND("Central Puget Sound Hub", 1),
    NORTH_SOUND("North Sound & Whidbey", 2),
    SAN_JUAN("San Juan Islands Network", 3)
}

data class WsfTerminal(
    val code: String,
    val name: String,
    val corridor: WsfCorridor,
    val corridorIndex: Int, // Strict monotonic progression 0..18 South -> North
    val latitude: Double,
    val longitude: Double,
    val slipsCount: Int,
    val defaultStagingBay: String,
    val connectedTerminals: List<String>
)

object WsfNetworkCatalog {
    // Full 19-terminal Washington State Ferries network ordered monotonically South -> North
    // so warehouse delivery routes can be sequenced with zero backtracking.
    val terminals: List<WsfTerminal> = listOf(
        WsfTerminal(
            code = "PTD",
            name = "Point Defiance (Tacoma)",
            corridor = WsfCorridor.SOUTH_SOUND,
            corridorIndex = 0,
            latitude = 47.3065,
            longitude = -122.5140,
            slipsCount = 1,
            defaultStagingBay = "Bay S-1 • Tacoma Dock",
            connectedTerminals = listOf("TAH")
        ),
        WsfTerminal(
            code = "TAH",
            name = "Tahlequah (Vashon South)",
            corridor = WsfCorridor.SOUTH_SOUND,
            corridorIndex = 1,
            latitude = 47.3319,
            longitude = -122.5078,
            slipsCount = 1,
            defaultStagingBay = "Bay S-2 • South Staging",
            connectedTerminals = listOf("PTD", "VAS")
        ),
        WsfTerminal(
            code = "SOU",
            name = "Southworth",
            corridor = WsfCorridor.SOUTH_SOUND,
            corridorIndex = 2,
            latitude = 47.5131,
            longitude = -122.4998,
            slipsCount = 1,
            defaultStagingBay = "Bay S-3 • Kitsap East",
            connectedTerminals = listOf("VAS", "FAU")
        ),
        WsfTerminal(
            code = "VAS",
            name = "Vashon Island (North)",
            corridor = WsfCorridor.SOUTH_SOUND,
            corridorIndex = 3,
            latitude = 47.5108,
            longitude = -122.4636,
            slipsCount = 2,
            defaultStagingBay = "Bay S-4 • Island Freight",
            connectedTerminals = listOf("SOU", "FAU", "TAH")
        ),
        WsfTerminal(
            code = "FAU",
            name = "Fauntleroy (West Seattle)",
            corridor = WsfCorridor.SOUTH_SOUND,
            corridorIndex = 4,
            latitude = 47.5232,
            longitude = -122.3962,
            slipsCount = 1,
            defaultStagingBay = "Bay S-5 • West Transfer",
            connectedTerminals = listOf("VAS", "SOU", "SEA")
        ),
        WsfTerminal(
            code = "BRE",
            name = "Bremerton",
            corridor = WsfCorridor.CENTRAL_SOUND,
            corridorIndex = 5,
            latitude = 47.5618,
            longitude = -122.6228,
            slipsCount = 2,
            defaultStagingBay = "Bay C-1 • Naval Yard Freight",
            connectedTerminals = listOf("SEA", "BBI")
        ),
        WsfTerminal(
            code = "SEA",
            name = "Seattle (Colman Dock Pier 52)",
            corridor = WsfCorridor.CENTRAL_SOUND,
            corridorIndex = 6,
            latitude = 47.6026,
            longitude = -122.3385,
            slipsCount = 3,
            defaultStagingBay = "Bay C-2 • Pier 52 Commercial",
            connectedTerminals = listOf("BBI", "BRE", "EDM")
        ),
        WsfTerminal(
            code = "BBI",
            name = "Bainbridge Island (Eagle Harbor)",
            corridor = WsfCorridor.CENTRAL_SOUND,
            corridorIndex = 7,
            latitude = 47.6223,
            longitude = -122.5096,
            slipsCount = 2,
            defaultStagingBay = "Bay C-3 • Eagle Maintenance",
            connectedTerminals = listOf("SEA", "KIN")
        ),
        WsfTerminal(
            code = "EDM",
            name = "Edmonds",
            corridor = WsfCorridor.CENTRAL_SOUND,
            corridorIndex = 8,
            latitude = 47.8134,
            longitude = -122.3853,
            slipsCount = 1,
            defaultStagingBay = "Bay C-4 • North King Freight",
            connectedTerminals = listOf("KIN", "MUK")
        ),
        WsfTerminal(
            code = "KIN",
            name = "Kingston",
            corridor = WsfCorridor.CENTRAL_SOUND,
            corridorIndex = 9,
            latitude = 47.7947,
            longitude = -122.4945,
            slipsCount = 2,
            defaultStagingBay = "Bay C-5 • Appletree Slip",
            connectedTerminals = listOf("EDM", "PTT")
        ),
        WsfTerminal(
            code = "MUK",
            name = "Mukilteo",
            corridor = WsfCorridor.NORTH_SOUND,
            corridorIndex = 10,
            latitude = 47.9495,
            longitude = -122.3046,
            slipsCount = 2,
            defaultStagingBay = "Bay N-1 • Snohomish Cargo",
            connectedTerminals = listOf("CLI")
        ),
        WsfTerminal(
            code = "CLI",
            name = "Clinton (Whidbey South)",
            corridor = WsfCorridor.NORTH_SOUND,
            corridorIndex = 11,
            latitude = 47.9754,
            longitude = -122.3518,
            slipsCount = 2,
            defaultStagingBay = "Bay N-2 • SR-525 Freight",
            connectedTerminals = listOf("MUK", "COU")
        ),
        WsfTerminal(
            code = "PTT",
            name = "Port Townsend",
            corridor = WsfCorridor.NORTH_SOUND,
            corridorIndex = 12,
            latitude = 48.1128,
            longitude = -122.7590,
            slipsCount = 1,
            defaultStagingBay = "Bay N-3 • Olympic Peninsula",
            connectedTerminals = listOf("COU", "KIN")
        ),
        WsfTerminal(
            code = "COU",
            name = "Coupeville (Keystone)",
            corridor = WsfCorridor.NORTH_SOUND,
            corridorIndex = 13,
            latitude = 48.1592,
            longitude = -122.6725,
            slipsCount = 1,
            defaultStagingBay = "Bay N-4 • Admiralty Inlet",
            connectedTerminals = listOf("PTT", "ANA")
        ),
        WsfTerminal(
            code = "ANA",
            name = "Anacortes (Ship Harbor)",
            corridor = WsfCorridor.SAN_JUAN,
            corridorIndex = 14,
            latitude = 48.5073,
            longitude = -122.6776,
            slipsCount = 2,
            defaultStagingBay = "Bay SJ-1 • Main Island Hub",
            connectedTerminals = listOf("LOP", "SHW", "ORC", "FRH")
        ),
        WsfTerminal(
            code = "LOP",
            name = "Lopez Island",
            corridor = WsfCorridor.SAN_JUAN,
            corridorIndex = 15,
            latitude = 48.5709,
            longitude = -122.8829,
            slipsCount = 1,
            defaultStagingBay = "Bay SJ-2 • Upright Head",
            connectedTerminals = listOf("ANA", "SHW", "ORC", "FRH")
        ),
        WsfTerminal(
            code = "SHW",
            name = "Shaw Island",
            corridor = WsfCorridor.SAN_JUAN,
            corridorIndex = 16,
            latitude = 48.5847,
            longitude = -122.9296,
            slipsCount = 1,
            defaultStagingBay = "Bay SJ-3 • Blind Bay Dock",
            connectedTerminals = listOf("ANA", "LOP", "ORC")
        ),
        WsfTerminal(
            code = "ORC",
            name = "Orcas Island",
            corridor = WsfCorridor.SAN_JUAN,
            corridorIndex = 17,
            latitude = 48.5974,
            longitude = -122.9438,
            slipsCount = 1,
            defaultStagingBay = "Bay SJ-4 • Orcas Landing",
            connectedTerminals = listOf("ANA", "LOP", "SHW", "FRH")
        ),
        WsfTerminal(
            code = "FRH",
            name = "Friday Harbor (San Juan Is.)",
            corridor = WsfCorridor.SAN_JUAN,
            corridorIndex = 18,
            latitude = 48.5359,
            longitude = -123.0139,
            slipsCount = 2,
            defaultStagingBay = "Bay SJ-5 • Front Street Slip",
            connectedTerminals = listOf("ANA", "ORC", "LOP")
        )
    )

    private val terminalByCode = terminals.associateBy { it.code }

    fun getTerminal(code: String): WsfTerminal =
        terminalByCode[code] ?: terminals[6] // Default to SEA Pier 52

    fun calculateDistanceMiles(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return ((earthRadiusMiles * c) * 10.0).roundToInt() / 10.0
    }

    fun calculateBearingDegrees(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = Math.toDegrees(atan2(y, x))
        return ((theta + 360) % 360).roundToInt()
    }

    fun findNearestTerminal(lat: Double, lon: Double): Pair<WsfTerminal, Double> {
        val closest = terminals.minByOrNull {
            calculateDistanceMiles(lat, lon, it.latitude, it.longitude)
        } ?: terminals[6]
        val dist = calculateDistanceMiles(lat, lon, closest.latitude, closest.longitude)
        return closest to dist
    }
}

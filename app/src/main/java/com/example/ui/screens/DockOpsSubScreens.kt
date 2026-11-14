package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.WsfNetworkCatalog
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.FerrySailingEntity
import com.example.data.local.SyncOperationEntity
import com.example.service.LiveGpsTelemetry
import com.example.ui.components.PugetSoundRadarCanvas
import com.example.ui.theme.BeaconAmberHigh
import com.example.ui.theme.ClearedEmerald
import com.example.ui.theme.PugetDeepNavy
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafetyOrangeCritical
import com.example.ui.theme.WsfMarineTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VesselScheduleScreen(
    sailings: List<FerrySailingEntity>,
    onDispatchUrgentNotification: () -> Unit,
    onNavigateBackToDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBackToDeck() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("vessel_schedule_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(1.dp, RadarCyan.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WSF ACTIVE FLEET & FREIGHT DECK CAPACITY",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Live arrival countdowns, commercial car-deck weight utilization, and urgent push dispatch across all Puget Sound corridors.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onDispatchUrgentNotification,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("dispatch_urgent_push_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafetyOrangeCritical,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIMULATE URGENT VESSEL / CARGO PUSH ALERT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        items(sailings, key = { it.id }) { sailing ->
            val loadRatio = (sailing.currentAssignedLbs.toFloat() / sailing.maxFreightDeckLbs.toFloat())
                .coerceIn(0f, 1f)
            val statusColor = when (sailing.statusBadge) {
                "ARRIVING_NOW" -> ClearedEmerald
                "TIGHT_CUTOFF" -> SafetyOrangeCritical
                "DELAYED_TIDAL" -> BeaconAmberHigh
                else -> RadarCyan
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sailing_card_${sailing.vesselName}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBoat,
                                contentDescription = null,
                                tint = WsfMarineTeal
                            )
                            Column {
                                Text(
                                    text = sailing.vesselName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${sailing.originTerminalCode} → ${sailing.destinationTerminalCode} • Slip ${sailing.slipNumber} • ${sailing.corridorName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, statusColor)
                        ) {
                            Text(
                                text = "ARR ${sailing.arrivalInMinutes} MIN",
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = sailing.advisoryNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "COMMERCIAL DECK LOAD: ${sailing.currentAssignedLbs} / ${sailing.maxFreightDeckLbs} LBS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(loadRatio * 100).toInt()}% FULL • ${sailing.crossingMinutes}m CROSSING",
                            style = MaterialTheme.typography.labelMedium,
                            color = WsfMarineTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { loadRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (loadRatio > 0.8f) SafetyOrangeCritical else WsfMarineTeal,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GpsCorridorRadarScreen(
    gpsTelemetry: LiveGpsTelemetry,
    activeStops: List<DeliveryStopEntity>,
    onRequestLocationPermission: () -> Unit,
    onJumpGpsToTerminal: (String) -> Unit,
    onNavigateBackToDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBackToDeck() }

    val terminals = WsfNetworkCatalog.terminals
    var inspectedTerminalCode by remember { mutableStateOf(gpsTelemetry.nearestTerminalCode) }
    val inspectedTerminal = WsfNetworkCatalog.getTerminal(inspectedTerminalCode)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("gps_radar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, RadarCyan.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = gpsTelemetry.telemetrySourceLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = RadarCyan
                            )
                            Text(
                                text = "NEAREST SLIP: ${gpsTelemetry.nearestTerminalCode} (${gpsTelemetry.distanceToNearestMiles} mi)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!gpsTelemetry.hasHardwarePermission) {
                            Button(
                                onClick = onRequestLocationPermission,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("grant_gps_permission_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RadarCyan,
                                    contentColor = PugetDeepNavy
                                )
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ENABLE HARDWARE GPS", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryStatBlock(
                            label = "COORDINATES",
                            value = String.format(
                                Locale.US,
                                "%.4f°N, %.4f°W",
                                gpsTelemetry.latitude,
                                -gpsTelemetry.longitude
                            )
                        )
                        TelemetryStatBlock(
                            label = "SPEED / HEADING",
                            value = "${gpsTelemetry.speedMph} mph (${gpsTelemetry.speedKnots} kt) • ${gpsTelemetry.bearingDegrees}°"
                        )
                        TelemetryStatBlock(
                            label = "NEXT SLIP ETA",
                            value = "${gpsTelemetry.etaToTargetMinutes}m (${gpsTelemetry.distanceToTargetSlipMiles} mi)"
                        )
                    }
                }
            }
        }

        item {
            PugetSoundRadarCanvas(
                terminals = terminals,
                activeStops = activeStops,
                gpsTelemetry = gpsTelemetry,
                selectedTerminalCode = inspectedTerminalCode,
                onSelectTerminal = { tapped ->
                    inspectedTerminalCode = tapped.code
                    onJumpGpsToTerminal(tapped.code)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "INSPECTED TERMINAL: ${inspectedTerminal.code} — ${inspectedTerminal.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = BeaconAmberHigh
                    )
                    Text(
                        text = "${inspectedTerminal.corridor.displayName} • Progression Index #${inspectedTerminal.corridorIndex + 1}/19 • ${inspectedTerminal.slipsCount} Ferry Slips • ${inspectedTerminal.defaultStagingBay}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "STEP GPS BEACON ALONG PUGET SOUND TERMINALS (GLOVED TAP):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        terminals.forEach { term ->
                            FilterChip(
                                selected = inspectedTerminalCode == term.code,
                                onClick = {
                                    inspectedTerminalCode = term.code
                                    onJumpGpsToTerminal(term.code)
                                },
                                label = {
                                    Text(
                                        text = term.code,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("gps_beacon_chip_${term.code}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatBlock(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = WsfMarineTeal
        )
    }
}

@Composable
fun OfflineSyncQueueScreen(
    isOnline: Boolean,
    isManualDeadZoneEnabled: Boolean,
    isSyncFlushing: Boolean,
    pendingSyncCount: Int,
    syncOperations: List<SyncOperationEntity>,
    onToggleDeadZoneOverride: () -> Unit,
    onFlushOfflineQueue: () -> Unit,
    onClearSyncedHistory: () -> Unit,
    onNavigateBackToDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBackToDeck() }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("offline_sync_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (isOnline) ClearedEmerald else BeaconAmberHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) ClearedEmerald else BeaconAmberHigh,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = if (isOnline) {
                                        "ONLINE • LIVE WSF DISPATCH SYNC"
                                    } else {
                                        "OFFLINE • STEEL HULL / WAREHOUSE DEAD ZONE"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isOnline) ClearedEmerald else BeaconAmberHigh
                                )
                                Text(
                                    text = "$pendingSyncCount Pending Outbox Mutations • ${syncOperations.size} Total Logged Events",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Manual Steel Hull Dead-Zone Simulation Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = BeaconAmberHigh
                            )
                            Column {
                                Text(
                                    text = "Simulate Ferry Car-Deck / Warehouse Dead Zone",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Forces local Room queueing for gloved status & checklist edits",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isManualDeadZoneEnabled,
                            onCheckedChange = { onToggleDeadZoneOverride() },
                            modifier = Modifier.testTag("deadzone_override_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onFlushOfflineQueue,
                            enabled = !isSyncFlushing,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(54.dp)
                                .testTag("flush_sync_queue_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WsfMarineTeal,
                                contentColor = PugetDeepNavy
                            )
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSyncFlushing) "RECONCILING..." else "FLUSH OFFLINE QUEUE ($pendingSyncCount)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onClearSyncedHistory,
                            modifier = Modifier
                                .weight(0.7f)
                                .height(54.dp)
                                .testTag("clear_synced_history_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PRUNE LOG", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        items(syncOperations, key = { it.id }) { op ->
            val isPending = op.syncState == "PENDING"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isPending) BeaconAmberHigh else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isPending) {
                                    BeaconAmberHigh.copy(alpha = 0.2f)
                                } else {
                                    ClearedEmerald.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = op.syncState,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isPending) BeaconAmberHigh else ClearedEmerald,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${op.operationType} • ${op.manifestCode}",
                                style = MaterialTheme.typography.labelLarge,
                                color = WsfMarineTeal
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = op.summaryText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = timeFormatter.format(Date(op.createdAtMillis)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

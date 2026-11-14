package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.WsfCorridor
import com.example.data.local.CargoManifestItemEntity
import com.example.data.local.DeliveryStopEntity
import com.example.domain.CorridorDirection
import com.example.domain.RouteOptimizationMetrics
import com.example.domain.RoutePriorityMode
import com.example.service.UrgentDispatchAlert
import com.example.ui.components.DeliveryStopCard
import com.example.ui.theme.BeaconAmberHigh
import com.example.ui.theme.PugetDeepNavy
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafetyOrangeCritical
import com.example.ui.theme.WsfMarineTeal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RouteDeckScreen(
    stops: List<DeliveryStopEntity>,
    cargoItemsByStopId: Map<Int, List<CargoManifestItemEntity>>,
    routeMetrics: RouteOptimizationMetrics,
    priorityMode: RoutePriorityMode,
    corridorDirection: CorridorDirection,
    selectedCorridorFilter: WsfCorridor?,
    activeUrgentAlert: UrgentDispatchAlert?,
    onSelectPriorityMode: (RoutePriorityMode) -> Unit,
    onToggleCorridorDirection: () -> Unit,
    onOptimizeNoBacktrack: () -> Unit,
    onSelectCorridorFilter: (WsfCorridor?) -> Unit,
    onGlovedStatusAdvance: (DeliveryStopEntity) -> Unit,
    onOpenManifestDetail: (Int) -> Unit,
    onDismissUrgentAlert: () -> Unit,
    onResetShiftData: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("route_deck_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Maritime Hero Banner + Live Zero-Backtrack Corridor Metrics
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, WsfMarineTeal.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_wsf_hero_banner_1793978032048),
                        contentDescription = "Washington State Ferry docking at Puget Sound terminal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        PugetDeepNavy.copy(alpha = 0.45f),
                                        PugetDeepNavy.copy(alpha = 0.85f),
                                        PugetDeepNavy.copy(alpha = 0.96f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = WsfMarineTeal.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, WsfMarineTeal)
                            ) {
                                Text(
                                    text = "ZERO-BACKTRACK CORRIDOR: ${routeMetrics.corridorSpanSummary}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = WsfMarineTeal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = RadarCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "19 WSF TERMINALS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadarCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Puget Sound Freight Routing Deck",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HeroMetricPill(
                                    label = "ACTIVE STOPS",
                                    value = "${routeMetrics.activeStopCount} (${routeMetrics.completedStopCount} DONE)",
                                    valueColor = WsfMarineTeal
                                )
                                HeroMetricPill(
                                    label = "LOOPS PRUNED",
                                    value = "${routeMetrics.backtracksEliminatedCount} (-${routeMetrics.deadheadMilesSaved} mi)",
                                    valueColor = RadarCyan
                                )
                                HeroMetricPill(
                                    label = "NEXT CUTOFF",
                                    value = "${routeMetrics.earliestVesselCutoffMinutes} MIN",
                                    valueColor = BeaconAmberHigh
                                )
                                HeroMetricPill(
                                    label = "DECK LOAD",
                                    value = "${routeMetrics.totalActiveWeightLbs} LBS",
                                    valueColor = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Urgent Push Notification Ticker Banner
        item {
            AnimatedVisibility(visible = activeUrgentAlert != null) {
                if (activeUrgentAlert != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("urgent_alert_banner"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SafetyOrangeCritical.copy(alpha = 0.16f)
                        ),
                        border = BorderStroke(1.5.dp, SafetyOrangeCritical)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = "Urgent Alert",
                                        tint = SafetyOrangeCritical
                                    )
                                    Text(
                                        text = activeUrgentAlert.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SafetyOrangeCritical,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = onDismissUrgentAlert,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("dismiss_urgent_alert_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Alert",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Text(
                                text = activeUrgentAlert.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. No-Backtracking Route Optimizer & Priority Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROUTING PRIORITY & CORRIDOR DIRECTION",
                            style = MaterialTheme.typography.labelLarge,
                            color = WsfMarineTeal
                        )

                        OutlinedButton(
                            onClick = onToggleCorridorDirection,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_corridor_direction_btn"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, RadarCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Reverse Corridor Flow",
                                tint = RadarCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = corridorDirection.shortTag,
                                style = MaterialTheme.typography.labelLarge,
                                color = RadarCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoutePriorityMode.entries.forEach { mode ->
                            FilterChip(
                                selected = priorityMode == mode,
                                onClick = { onSelectPriorityMode(mode) },
                                label = {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WsfMarineTeal.copy(alpha = 0.25f),
                                    selectedLabelColor = WsfMarineTeal
                                ),
                                modifier = Modifier.testTag("priority_chip_${mode.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onOptimizeNoBacktrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("optimize_no_backtrack_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WsfMarineTeal,
                            contentColor = PugetDeepNavy
                        )
                    ) {
                        Icon(Icons.Default.AltRoute, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "RE-SEQUENCE ROUTE • ZERO BACKTRACKING",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Puget Sound Corridor Filter Chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCorridorFilter == null,
                    onClick = { onSelectCorridorFilter(null) },
                    label = {
                        Text("All Corridors", style = MaterialTheme.typography.labelMedium)
                    },
                    modifier = Modifier.testTag("filter_corridor_ALL")
                )
                WsfCorridor.entries.forEach { corridor ->
                    FilterChip(
                        selected = selectedCorridorFilter == corridor,
                        onClick = { onSelectCorridorFilter(corridor) },
                        label = {
                            Text(corridor.displayName, style = MaterialTheme.typography.labelMedium)
                        },
                        modifier = Modifier.testTag("filter_corridor_${corridor.name}")
                    )
                }
            }
        }

        // 5. Delivery Manifest Cards or Empty State
        if (stops.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBoat,
                            contentDescription = null,
                            tint = WsfMarineTeal,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Deliveries in Selected Corridor",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Clear the corridor filter or reload the default Puget Sound WSF shift schedule.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onResetShiftData,
                            modifier = Modifier.testTag("reset_shift_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RELOAD PUGET SOUND SHIFT")
                        }
                    }
                }
            }
        } else {
            items(stops, key = { it.id }) { stop ->
                val stopCargo = cargoItemsByStopId[stop.id].orEmpty()
                DeliveryStopCard(
                    stop = stop,
                    cargoItems = stopCargo,
                    onGlovedStatusAdvance = { onGlovedStatusAdvance(stop) },
                    onOpenManifestChecklist = { onOpenManifestDetail(stop.id) }
                )
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCBD5E1)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WsfNetworkCatalog
import com.example.data.local.CargoManifestItemEntity
import com.example.data.local.CargoUrgency
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.DeliveryWorkflowStatus
import com.example.ui.theme.BeaconAmberHigh
import com.example.ui.theme.ClearedEmerald
import com.example.ui.theme.PugetDeepNavy
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafetyOrangeCritical
import com.example.ui.theme.WsfMarineTeal

@Composable
fun DeliveryStopCard(
    stop: DeliveryStopEntity,
    cargoItems: List<CargoManifestItemEntity>,
    onGlovedStatusAdvance: () -> Unit,
    onOpenManifestChecklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originTerminal = WsfNetworkCatalog.getTerminal(stop.originTerminalCode)
    val destTerminal = WsfNetworkCatalog.getTerminal(stop.destinationTerminalCode)
    val urgency = stop.urgencyEnum
    val status = stop.statusEnum
    val isDelivered = status == DeliveryWorkflowStatus.DELIVERED

    val urgencyAccent = when (urgency) {
        CargoUrgency.CRITICAL -> SafetyOrangeCritical
        CargoUrgency.HIGH -> BeaconAmberHigh
        CargoUrgency.STANDARD -> RadarCyan
    }

    val checkedCount = cargoItems.count { it.isChecked }
    val totalCount = cargoItems.size.coerceAtLeast(1)
    val checkedWeight = cargoItems.filter { it.isChecked }.sumOf { it.weightLbs }
    val checklistProgress = checkedCount.toFloat() / totalCount.toFloat()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("delivery_stop_card_${stop.manifestCode}")
            .clickable { onOpenManifestChecklist() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDelivered) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (urgency == CargoUrgency.CRITICAL && !isDelivered) 2.dp else 1.dp,
            color = if (isDelivered) {
                ClearedEmerald.copy(alpha = 0.45f)
            } else {
                urgencyAccent.copy(alpha = 0.65f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Sequence Badge, Manifest Code, Urgency Pill, and Sync Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isDelivered) ClearedEmerald else urgencyAccent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "STOP #${stop.sequenceOrder}",
                            style = MaterialTheme.typography.labelLarge,
                            color = PugetDeepNavy,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = stop.manifestCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = urgencyAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, urgencyAccent.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = urgency.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = urgencyAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Icon(
                        imageVector = if (stop.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = if (stop.isSynced) "Synced" else "Queued Offline",
                        tint = if (stop.isSynced) ClearedEmerald else BeaconAmberHigh,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Terminal Pair & Assigned WSF Vessel Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stop.originTerminalCode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "To destination terminal",
                            tint = WsfMarineTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stop.destinationTerminalCode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = WsfMarineTeal
                        )
                    }
                    Text(
                        text = "${originTerminal.name} → ${destTerminal.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Vessel Arrival & Cutoff Badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBoat,
                                contentDescription = "Assigned Ferry",
                                tint = RadarCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "${stop.assignedVesselName} • SLIP ${stop.slipNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = RadarCyan
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Vessel Arrival Window",
                                tint = if (stop.boardingCutoffMinutes <= 8) SafetyOrangeCritical else BeaconAmberHigh,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ARR ${stop.vesselArrivalMinutes}m • CUTOFF ${stop.boardingCutoffMinutes}m",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (stop.boardingCutoffMinutes <= 8) SafetyOrangeCritical else BeaconAmberHigh
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Zero-Backtracking Corridor Note & Staging Bay
            Text(
                text = "${stop.stagingBayNote} • ${stop.backtrackPrunedNote}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cargo Manifest Weight & Checklist Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANIFEST CHECKLIST: $checkedCount/${cargoItems.size} PALLETS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$checkedWeight / ${stop.totalWeightLbs} LBS VERIFIED",
                        style = MaterialTheme.typography.labelMedium,
                        color = WsfMarineTeal
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { checklistProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = WsfMarineTeal,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Workflow Stepper Pills (5 stages)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DeliveryWorkflowStatus.entries.forEach { step ->
                    val reached = status.stepIndex >= step.stepIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (reached) {
                                    if (isDelivered) ClearedEmerald else WsfMarineTeal
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "CURRENT STATUS: ${status.displayLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isDelivered) ClearedEmerald else WsfMarineTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gloved-Hand High-Contrast Action Row (Minimum 56dp height for gloved operation)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onGlovedStatusAdvance,
                    modifier = Modifier
                        .weight(1.45f)
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("gloved_status_btn_${stop.manifestCode}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isDelivered -> MaterialTheme.colorScheme.surfaceVariant
                            urgency == CargoUrgency.CRITICAL -> SafetyOrangeCritical
                            else -> WsfMarineTeal
                        },
                        contentColor = if (isDelivered) {
                            MaterialTheme.colorScheme.onSurface
                        } else if (urgency == CargoUrgency.CRITICAL) {
                            Color.White
                        } else {
                            PugetDeepNavy
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isDelivered) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                        contentDescription = "Gloved One-Tap Action",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status.glovedButtonActionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenManifestChecklist,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(56.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("open_manifest_btn_${stop.manifestCode}"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, WsfMarineTeal.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "Open Cargo Checklist",
                        tint = WsfMarineTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PALLETS (${cargoItems.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

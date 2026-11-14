package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.WsfNetworkCatalog
import com.example.data.local.CargoManifestItemEntity
import com.example.data.local.CargoUrgency
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.FerrySailingEntity
import com.example.ui.theme.BeaconAmberHigh
import com.example.ui.theme.ClearedEmerald
import com.example.ui.theme.PugetDeepNavy
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafetyOrangeCritical
import com.example.ui.theme.WsfMarineTeal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CargoManifestDetailSheet(
    stop: DeliveryStopEntity,
    cargoItems: List<CargoManifestItemEntity>,
    availableSailings: List<FerrySailingEntity>,
    onToggleCargoItem: (CargoManifestItemEntity) -> Unit,
    onAdvanceGlovedStatus: () -> Unit,
    onReassignVessel: (FerrySailingEntity) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val originTerminal = WsfNetworkCatalog.getTerminal(stop.originTerminalCode)
    val destTerminal = WsfNetworkCatalog.getTerminal(stop.destinationTerminalCode)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stop.manifestCode} • STOP #${stop.sequenceOrder}",
                        style = MaterialTheme.typography.labelLarge,
                        color = WsfMarineTeal
                    )
                    Text(
                        text = "${originTerminal.code} → ${destTerminal.code} (${destTerminal.name})",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_manifest_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Manifest Detail"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${stop.assignedVesselName} (Slip ${stop.slipNumber}) • Arrives in ${stop.vesselArrivalMinutes} min • Cutoff ${stop.boardingCutoffMinutes} min",
                style = MaterialTheme.typography.labelMedium,
                color = RadarCyan
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Gloved One-Tap Primary Action Button inside Sheet
            Button(
                onClick = onAdvanceGlovedStatus,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("sheet_gloved_status_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WsfMarineTeal,
                    contentColor = PugetDeepNavy
                )
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Advance Status",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stop.statusEnum.glovedButtonActionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CARGO MANIFEST CHECKLIST (${cargoItems.sumOf { it.weightLbs }} LBS TOTAL)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            cargoItems.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("cargo_item_row_${item.palletSku}")
                        .clickable { onToggleCargoItem(item) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (item.isChecked) ClearedEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (item.isChecked) {
                                    Icons.Default.CheckBox
                                } else {
                                    Icons.Default.CheckBoxOutlineBlank
                                },
                                contentDescription = "Toggle Pallet Verified",
                                tint = if (item.isChecked) ClearedEmerald else WsfMarineTeal,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.palletSku,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (item.isUrgentItem) SafetyOrangeCritical else RadarCyan
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.handlingTag,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = BeaconAmberHigh,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.itemTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "${item.weightLbs} lbs",
                            style = MaterialTheme.typography.labelLarge,
                            color = WsfMarineTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ROUTE TO ALTERNATE WSF VESSEL (NO BACKTRACKING)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableSailings.forEach { sailing ->
                    val isCurrent = sailing.vesselName == stop.assignedVesselName &&
                        sailing.destinationTerminalCode == stop.destinationTerminalCode
                    FilterChip(
                        selected = isCurrent,
                        onClick = { onReassignVessel(sailing) },
                        label = {
                            Text(
                                text = "${sailing.vesselName} (${sailing.originTerminalCode}→${sailing.destinationTerminalCode} • ${sailing.arrivalInMinutes}m)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DirectionsBoat,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WsfMarineTeal.copy(alpha = 0.25f),
                            selectedLabelColor = WsfMarineTeal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewDeliveryManifestSheet(
    sailings: List<FerrySailingEntity>,
    onCreateManifest: (
        originCode: String,
        destinationCode: String,
        vesselName: String,
        vesselArrivalMin: Int,
        urgency: CargoUrgency,
        cargoDescription: String,
        weightLbs: Int
    ) -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val terminals = WsfNetworkCatalog.terminals

    var selectedOrigin by remember { mutableStateOf("SEA") }
    var selectedDest by remember { mutableStateOf("BBI") }
    var selectedUrgency by remember { mutableStateOf(CargoUrgency.HIGH) }
    var cargoDesc by remember { mutableStateOf("Marine Switchgear & Cold-Chain Provisions") }
    var weightInput by remember { mutableStateOf("4600") }

    val matchedSailing = sailings.firstOrNull {
        it.originTerminalCode == selectedOrigin && it.destinationTerminalCode == selectedDest
    } ?: sailings.firstOrNull { it.destinationTerminalCode == selectedDest }
        ?: sailings.firstOrNull()

    val vesselName = matchedSailing?.vesselName ?: "M/V Tacoma"
    val arrivalMin = matchedSailing?.arrivalInMinutes ?: 18

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEW WSF FREIGHT MANIFEST",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "1. ORIGIN WSF TERMINAL",
                style = MaterialTheme.typography.labelLarge,
                color = WsfMarineTeal
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("PTD", "FAU", "SEA", "EDM", "MUK", "COU", "ANA").forEach { code ->
                    FilterChip(
                        selected = selectedOrigin == code,
                        onClick = {
                            selectedOrigin = code
                            val originTerm = WsfNetworkCatalog.getTerminal(code)
                            originTerm.connectedTerminals.firstOrNull()?.let { connected ->
                                selectedDest = connected
                            }
                        },
                        label = { Text(code, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "2. DESTINATION WSF TERMINAL (19 PUGET SOUND TERMINALS)",
                style = MaterialTheme.typography.labelLarge,
                color = WsfMarineTeal
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                terminals.filter { it.code != selectedOrigin }.forEach { term ->
                    FilterChip(
                        selected = selectedDest == term.code,
                        onClick = { selectedDest = term.code },
                        label = {
                            Text(
                                "${term.code} (#${term.corridorIndex + 1})",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "3. CARGO URGENCY PRIORITY",
                style = MaterialTheme.typography.labelLarge,
                color = WsfMarineTeal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CargoUrgency.entries.forEach { urg ->
                    FilterChip(
                        selected = selectedUrgency == urg,
                        onClick = { selectedUrgency = urg },
                        label = { Text(urg.name, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = cargoDesc,
                onValueChange = { cargoDesc = it },
                label = { Text("Primary Pallet / Cargo Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_manifest_cargo_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it.filter { ch -> ch.isDigit() } },
                label = { Text("Total Freight Weight (LBS)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_manifest_weight_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Matched Vessel: $vesselName • Arrives in ${arrivalMin}m • Zero-Backtrack Auto-Sequenced",
                style = MaterialTheme.typography.labelMedium,
                color = RadarCyan
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val weight = weightInput.toIntOrNull()?.coerceIn(200, 40000) ?: 3500
                    val desc = cargoDesc.ifBlank { "WSF Priority Terminal Freight Crate" }
                    onCreateManifest(
                        selectedOrigin,
                        selectedDest,
                        vesselName,
                        arrivalMin,
                        selectedUrgency,
                        desc,
                        weight
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_new_manifest_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WsfMarineTeal,
                    contentColor = PugetDeepNavy
                )
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADD & SEQUENCE WITHOUT BACKTRACKING",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

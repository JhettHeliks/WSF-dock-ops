package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DockOpsTab
import com.example.ui.WsfLogisticsViewModel
import com.example.ui.components.CargoManifestDetailSheet
import com.example.ui.components.NewDeliveryManifestSheet
import com.example.ui.theme.BeaconAmberHigh
import com.example.ui.theme.ClearedEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PugetDeepNavy
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.SafetyOrangeCritical
import com.example.ui.theme.WsfMarineTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WsfDockOpsApp(
    viewModel: WsfLogisticsViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.onPermissionsUpdated()
    }

    fun requestPermissionsAndAction(afterRequest: () -> Unit) {
        val perms = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionsLauncher.launch(perms.toTypedArray())
        afterRequest()
    }

    LaunchedEffect(state.snackbarFeedback) {
        val msg = state.snackbarFeedback ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbarFeedback()
    }

    MyApplicationTheme(darkTheme = state.isDarkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 700.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "WSF DOCKOPS",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "GPS: ${state.gpsTelemetry.nearestTerminalCode} (${state.gpsTelemetry.distanceToNearestMiles} mi) • ETA ${state.gpsTelemetry.etaToTargetMinutes}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RadarCyan
                                )
                            }
                        },
                        actions = {
                            // Live Online / Offline Hull Status Pill
                            Surface(
                                color = if (state.isOnline) {
                                    ClearedEmerald.copy(alpha = 0.16f)
                                } else {
                                    BeaconAmberHigh.copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (state.isOnline) ClearedEmerald else BeaconAmberHigh
                                ),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("topbar_sync_status_pill")
                                    .clickable { viewModel.selectTab(DockOpsTab.OFFLINE_SYNC) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = if (state.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = "Connectivity Status",
                                        tint = if (state.isOnline) ClearedEmerald else BeaconAmberHigh,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (state.isOnline) {
                                            "SYNCED"
                                        } else {
                                            "OFFLINE (${state.pendingSyncCount})"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.isOnline) ClearedEmerald else BeaconAmberHigh
                                    )
                                }
                            }

                            // Trigger Urgent Push Notification Button
                            IconButton(
                                onClick = {
                                    requestPermissionsAndAction {
                                        viewModel.triggerUrgentPushNotification()
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_urgent_push_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Send Urgent WSF Push Notification",
                                    tint = SafetyOrangeCritical
                                )
                            }

                            // Tactical Night / Daylight Deck Contrast Toggle
                            IconButton(
                                onClick = { viewModel.toggleThemeMode() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("topbar_theme_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contrast,
                                    contentDescription = "Toggle Daylight / Night Bridge Theme",
                                    tint = WsfMarineTeal
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                floatingActionButton = {
                    if (state.selectedTab == DockOpsTab.ROUTE_DECK) {
                        ExtendedFloatingActionButton(
                            onClick = { viewModel.setShowNewDeliverySheet(true) },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = {
                                Text(
                                    text = "NEW MANIFEST",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            containerColor = WsfMarineTeal,
                            contentColor = PugetDeepNavy,
                            modifier = Modifier.testTag("fab_new_manifest")
                        )
                    }
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            DockOpsTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = state.selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        TabIconWithBadge(
                                            tab = tab,
                                            pendingSyncCount = state.pendingSyncCount
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            DockOpsTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = state.selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        TabIconWithBadge(
                                            tab = tab,
                                            pendingSyncCount = state.pendingSyncCount
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.testTag("rail_tab_${tab.route}")
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (state.selectedTab) {
                            DockOpsTab.ROUTE_DECK -> RouteDeckScreen(
                                stops = state.deliveryStops,
                                cargoItemsByStopId = state.cargoItemsByStopId,
                                routeMetrics = state.routeMetrics,
                                priorityMode = state.priorityMode,
                                corridorDirection = state.corridorDirection,
                                selectedCorridorFilter = state.selectedCorridorFilter,
                                activeUrgentAlert = state.activeUrgentAlert,
                                onSelectPriorityMode = viewModel::updatePriorityMode,
                                onToggleCorridorDirection = viewModel::toggleCorridorDirection,
                                onOptimizeNoBacktrack = { viewModel.optimizeRouteWithNoBacktracking() },
                                onSelectCorridorFilter = viewModel::setCorridorFilter,
                                onGlovedStatusAdvance = viewModel::advanceStopStatusGloved,
                                onOpenManifestDetail = viewModel::openManifestDetail,
                                onDismissUrgentAlert = viewModel::dismissUrgentAlert,
                                onResetShiftData = viewModel::resetShiftSampleData
                            )

                            DockOpsTab.VESSEL_SCHEDULE -> VesselScheduleScreen(
                                sailings = state.ferrySailings,
                                onDispatchUrgentNotification = {
                                    requestPermissionsAndAction {
                                        viewModel.triggerUrgentPushNotification()
                                    }
                                },
                                onNavigateBackToDeck = { viewModel.selectTab(DockOpsTab.ROUTE_DECK) }
                            )

                            DockOpsTab.GPS_CORRIDOR -> GpsCorridorRadarScreen(
                                gpsTelemetry = state.gpsTelemetry,
                                activeStops = state.deliveryStops,
                                onRequestLocationPermission = {
                                    requestPermissionsAndAction {
                                        viewModel.onPermissionsUpdated()
                                    }
                                },
                                onJumpGpsToTerminal = viewModel::jumpGpsToTerminal,
                                onNavigateBackToDeck = { viewModel.selectTab(DockOpsTab.ROUTE_DECK) }
                            )

                            DockOpsTab.OFFLINE_SYNC -> OfflineSyncQueueScreen(
                                isOnline = state.isOnline,
                                isManualDeadZoneEnabled = state.isManualDeadZoneEnabled,
                                isSyncFlushing = state.isSyncFlushing,
                                pendingSyncCount = state.pendingSyncCount,
                                syncOperations = state.syncOperations,
                                onToggleDeadZoneOverride = viewModel::toggleManualDeadZoneOverride,
                                onFlushOfflineQueue = { viewModel.flushOfflineQueueNow(false) },
                                onClearSyncedHistory = viewModel::clearSyncedQueueHistory,
                                onNavigateBackToDeck = { viewModel.selectTab(DockOpsTab.ROUTE_DECK) }
                            )
                        }
                    }
                }
            }

            // Cargo Manifest & Gloved Execution Sheet
            val selectedStop = state.selectedStopForManifest
            if (selectedStop != null) {
                val itemsForStop = state.cargoItemsByStopId[selectedStop.id].orEmpty()
                CargoManifestDetailSheet(
                    stop = selectedStop,
                    cargoItems = itemsForStop,
                    availableSailings = state.ferrySailings,
                    onToggleCargoItem = { item ->
                        viewModel.toggleCargoItemChecked(item, selectedStop.manifestCode)
                    },
                    onAdvanceGlovedStatus = {
                        viewModel.advanceStopStatusGloved(selectedStop)
                    },
                    onReassignVessel = { sailing ->
                        viewModel.reassignStopToVessel(selectedStop, sailing)
                    },
                    onDismiss = viewModel::closeManifestDetail
                )
            }

            // Add New Delivery Manifest Sheet
            if (state.showNewDeliverySheet) {
                NewDeliveryManifestSheet(
                    sailings = state.ferrySailings,
                    onCreateManifest = viewModel::createNewDeliveryManifest,
                    onDismiss = { viewModel.setShowNewDeliverySheet(false) }
                )
            }
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: DockOpsTab,
    pendingSyncCount: Int
) {
    val icon = when (tab) {
        DockOpsTab.ROUTE_DECK -> Icons.Default.AltRoute
        DockOpsTab.VESSEL_SCHEDULE -> Icons.Default.DirectionsBoat
        DockOpsTab.GPS_CORRIDOR -> Icons.Default.GpsFixed
        DockOpsTab.OFFLINE_SYNC -> Icons.Default.Sync
    }

    if (tab == DockOpsTab.OFFLINE_SYNC && pendingSyncCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = BeaconAmberHigh, contentColor = PugetDeepNavy) {
                    Text(pendingSyncCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}

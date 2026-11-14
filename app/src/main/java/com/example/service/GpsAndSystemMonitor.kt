package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.WsfNetworkCatalog
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class LiveGpsTelemetry(
    val latitude: Double = 47.6026, // Default Pier 52 Colman Dock Seattle
    val longitude: Double = -122.3385,
    val speedMph: Double = 18.4,
    val speedKnots: Double = 16.0,
    val bearingDegrees: Int = 312,
    val nearestTerminalCode: String = "SEA",
    val nearestTerminalName: String = "Seattle (Colman Dock Pier 52)",
    val distanceToNearestMiles: Double = 0.2,
    val distanceToTargetSlipMiles: Double = 4.6,
    val etaToTargetMinutes: Int = 11,
    val hasHardwarePermission: Boolean = false,
    val telemetrySourceLabel: String = "WSF CORRIDOR BEACON (PIER 52)"
)

data class UrgentDispatchAlert(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val message: String,
    val terminalCode: String,
    val vesselName: String,
    val severity: String = "CRITICAL" // CRITICAL or HIGH
)

class GpsAndSystemMonitor(private val context: Context) {

    companion object {
        const val URGENT_CHANNEL_ID = "wsf_dockops_urgent_channel"
    }

    private val _gpsTelemetry = MutableStateFlow(LiveGpsTelemetry())
    val gpsTelemetry: StateFlow<LiveGpsTelemetry> = _gpsTelemetry.asStateFlow()

    private val _hardwareOnline = MutableStateFlow(true)
    private val _manualDeadZoneOverride = MutableStateFlow(false)

    private val _isEffectiveOnline = MutableStateFlow(true)
    val isEffectiveOnline: StateFlow<Boolean> = _isEffectiveOnline.asStateFlow()

    val isManualDeadZoneEnabled: StateFlow<Boolean> = _manualDeadZoneOverride.asStateFlow()

    private val fusedLocationClient = runCatching {
        LocationServices.getFusedLocationProviderClient(context)
    }.getOrNull()

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var targetTerminalCode: String = "BBI"

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            onHardwareLocationReceived(loc)
        }
    }

    private val platformLocationListener = LocationListener { loc ->
        onHardwareLocationReceived(loc)
    }

    init {
        createUrgentNotificationChannel()
        registerConnectivityMonitor()
        refreshLocationTracking()
    }

    private fun registerConnectivityMonitor() {
        val cm = connectivityManager ?: return
        val activeNetwork = cm.activeNetwork
        val caps = activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _hardwareOnline.value = connected
        recomputeEffectiveOnline()

        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _hardwareOnline.value = true
                        recomputeEffectiveOnline()
                    }

                    override fun onLost(network: Network) {
                        _hardwareOnline.value = false
                        recomputeEffectiveOnline()
                    }
                }
            )
        }
    }

    fun setManualDeadZoneOverride(enabled: Boolean) {
        _manualDeadZoneOverride.value = enabled
        recomputeEffectiveOnline()
    }

    private fun recomputeEffectiveOnline() {
        _isEffectiveOnline.value = _hardwareOnline.value && !_manualDeadZoneOverride.value
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshLocationTracking() {
        val permitted = hasLocationPermission()
        _gpsTelemetry.value = _gpsTelemetry.value.copy(hasHardwarePermission = permitted)
        if (!permitted) return

        runCatching {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
                .setMinUpdateIntervalMillis(2000L)
                .build()
            fusedLocationClient?.requestLocationUpdates(
                request,
                fusedCallback,
                Looper.getMainLooper()
            )
            fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                if (loc != null) onHardwareLocationReceived(loc)
            }
        }

        runCatching {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                4000L,
                5f,
                platformLocationListener,
                Looper.getMainLooper()
            )
        }
    }

    fun updateTargetTerminal(code: String) {
        targetTerminalCode = code
        val current = _gpsTelemetry.value
        recalculateTelemetryForCoordinates(
            lat = current.latitude,
            lon = current.longitude,
            speedMph = current.speedMph,
            sourceLabel = current.telemetrySourceLabel
        )
    }

    /**
     * Allows warehouse workers or dispatchers testing indoors to step the active GPS beacon
     * directly to any Puget Sound WSF terminal along the forward corridor.
     */
    fun jumpGpsBeaconToTerminal(terminalCode: String) {
        val terminal = WsfNetworkCatalog.getTerminal(terminalCode)
        recalculateTelemetryForCoordinates(
            lat = terminal.latitude,
            lon = terminal.longitude,
            speedMph = 21.5,
            sourceLabel = "DOCKSIDE GPS LOCK • ${terminal.code}"
        )
    }

    private fun onHardwareLocationReceived(location: Location) {
        val mph = if (location.hasSpeed()) {
            ((location.speed * 2.23694) * 10.0).roundToInt() / 10.0
        } else {
            19.2
        }
        recalculateTelemetryForCoordinates(
            lat = location.latitude,
            lon = location.longitude,
            speedMph = mph.coerceAtLeast(4.0),
            sourceLabel = "HARDWARE GPS SATELLITE FIX"
        )
    }

    private fun recalculateTelemetryForCoordinates(
        lat: Double,
        lon: Double,
        speedMph: Double,
        sourceLabel: String
    ) {
        val (nearest, nearestDist) = WsfNetworkCatalog.findNearestTerminal(lat, lon)
        val targetTerminal = WsfNetworkCatalog.getTerminal(targetTerminalCode)
        val distToTarget = WsfNetworkCatalog.calculateDistanceMiles(
            lat,
            lon,
            targetTerminal.latitude,
            targetTerminal.longitude
        )
        val bearing = WsfNetworkCatalog.calculateBearingDegrees(
            lat,
            lon,
            targetTerminal.latitude,
            targetTerminal.longitude
        )
        val effectiveSpeed = speedMph.coerceAtLeast(12.0)
        val etaMin = ((distToTarget / effectiveSpeed) * 60.0).roundToInt().coerceAtLeast(2)
        val knots = ((effectiveSpeed * 0.868976) * 10.0).roundToInt() / 10.0

        _gpsTelemetry.value = LiveGpsTelemetry(
            latitude = lat,
            longitude = lon,
            speedMph = effectiveSpeed,
            speedKnots = knots,
            bearingDegrees = bearing,
            nearestTerminalCode = nearest.code,
            nearestTerminalName = nearest.name,
            distanceToNearestMiles = nearestDist,
            distanceToTargetSlipMiles = distToTarget,
            etaToTargetMinutes = etaMin,
            hasHardwarePermission = hasLocationPermission(),
            telemetrySourceLabel = sourceLabel
        )
    }

    private fun createUrgentNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                URGENT_CHANNEL_ID,
                "WSF Urgent Ferry & Cargo Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alerts for vessel arrival cutoffs, slip changes, and cold-chain freight."
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun sendUrgentPushNotification(alert: UrgentDispatchAlert) {
        if (!hasNotificationPermission()) return
        runCatching {
            val builder = NotificationCompat.Builder(context, URGENT_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setAutoCancel(true)

            NotificationManagerCompat.from(context)
                .notify((alert.id % 100000).toInt(), builder.build())
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WsfNetworkCatalog
import com.example.data.WsfTerminal
import com.example.data.local.DeliveryStopEntity
import com.example.data.local.DeliveryWorkflowStatus
import com.example.service.LiveGpsTelemetry
import com.example.ui.theme.BeaconAmberHigh
import com.example.ui.theme.PugetDeepNavy
import com.example.ui.theme.RadarCyan
import com.example.ui.theme.RobotoMonoFontFamily
import com.example.ui.theme.SafetyOrangeCritical
import com.example.ui.theme.WsfMarineTeal
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun PugetSoundRadarCanvas(
    terminals: List<WsfTerminal>,
    activeStops: List<DeliveryStopEntity>,
    gpsTelemetry: LiveGpsTelemetry,
    selectedTerminalCode: String,
    onSelectTerminal: (WsfTerminal) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "radarPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    // Geographic bounding box of Puget Sound & San Juan Islands
    val minLat = 47.24
    val maxLat = 48.66
    val minLon = -123.12
    val maxLon = -122.22

    fun projectToCanvas(lat: Double, lon: Double, width: Float, height: Float): Offset {
        val padX = width * 0.10f
        val padY = height * 0.08f
        val usableW = (width - padX * 2f).coerceAtLeast(1f)
        val usableH = (height - padY * 2f).coerceAtLeast(1f)

        val normX = ((lon - minLon) / (maxLon - minLon)).toFloat().coerceIn(0f, 1f)
        // Higher latitude is further North (top of canvas -> smaller Y)
        val normY = (1.0 - ((lat - minLat) / (maxLat - minLat))).toFloat().coerceIn(0f, 1f)
        return Offset(
            x = padX + normX * usableW,
            y = padY + normY * usableH
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF061014),
                        PugetDeepNavy,
                        Color(0xFF0A1D24)
                    )
                )
            )
            .border(1.dp, WsfMarineTeal.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(terminals) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val tapped = terminals.minByOrNull { term ->
                            val pt = projectToCanvas(term.latitude, term.longitude, w, h)
                            hypot(tapOffset.x - pt.x, tapOffset.y - pt.y)
                        }
                        if (tapped != null) {
                            onSelectTerminal(tapped)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Tactical Radar Grid & Range Rings
            val gridStepX = w / 6f
            val gridStepY = h / 6f
            for (i in 1..5) {
                drawLine(
                    color = WsfMarineTeal.copy(alpha = 0.08f),
                    start = Offset(gridStepX * i, 0f),
                    end = Offset(gridStepX * i, h),
                    strokeWidth = 1f
                )
                drawLine(
                    color = WsfMarineTeal.copy(alpha = 0.08f),
                    start = Offset(0f, gridStepY * i),
                    end = Offset(w, gridStepY * i),
                    strokeWidth = 1f
                )
            }

            // 2. Draw all WSF Ferry Corridor Connections (dashed maritime lanes)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            terminals.forEach { origin ->
                val p1 = projectToCanvas(origin.latitude, origin.longitude, w, h)
                origin.connectedTerminals.forEach { destCode ->
                    val dest = WsfNetworkCatalog.getTerminal(destCode)
                    if (origin.corridorIndex < dest.corridorIndex) {
                        val p2 = projectToCanvas(dest.latitude, dest.longitude, w, h)
                        drawLine(
                            color = RadarCyan.copy(alpha = 0.25f),
                            start = p1,
                            end = p2,
                            strokeWidth = 2.5f,
                            pathEffect = dashEffect
                        )
                    }
                }
            }

            // 3. Draw Active Zero-Backtrack Delivery Vectors with directional arrowheads
            val pendingStops = activeStops
                .filter { it.statusEnum != DeliveryWorkflowStatus.DELIVERED }
                .sortedBy { it.sequenceOrder }

            pendingStops.forEachIndexed { idx, stop ->
                val orig = WsfNetworkCatalog.getTerminal(stop.originTerminalCode)
                val dest = WsfNetworkCatalog.getTerminal(stop.destinationTerminalCode)
                val startPt = projectToCanvas(orig.latitude, orig.longitude, w, h)
                val endPt = projectToCanvas(dest.latitude, dest.longitude, w, h)

                val lineColor = when (idx) {
                    0 -> SafetyOrangeCritical
                    1 -> BeaconAmberHigh
                    else -> WsfMarineTeal
                }

                drawLine(
                    color = lineColor,
                    start = startPt,
                    end = endPt,
                    strokeWidth = 5.5f
                )

                // Draw directional arrowhead at 70% along the segment
                val midX = startPt.x + (endPt.x - startPt.x) * 0.72f
                val midY = startPt.y + (endPt.y - startPt.y) * 0.72f
                val angle = atan2(endPt.y - startPt.y, endPt.x - startPt.x)
                val arrowLen = 15f
                val arrowPath = Path().apply {
                    moveTo(midX, midY)
                    lineTo(
                        midX - arrowLen * cos(angle - 0.5f),
                        midY - arrowLen * sin(angle - 0.5f)
                    )
                    lineTo(
                        midX - arrowLen * cos(angle + 0.5f),
                        midY - arrowLen * sin(angle + 0.5f)
                    )
                    close()
                }
                drawPath(arrowPath, color = lineColor)
            }

            // 4. Draw Live Worker GPS Beacon & Pulsing Radar Ring
            val gpsPt = projectToCanvas(gpsTelemetry.latitude, gpsTelemetry.longitude, w, h)
            drawCircle(
                color = RadarCyan.copy(alpha = (1f - pulseProgress) * 0.55f),
                radius = 14f + (pulseProgress * 38f),
                center = gpsPt
            )
            drawCircle(
                color = RadarCyan,
                radius = 8.5f,
                center = gpsPt
            )
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = gpsPt
            )

            // 5. Draw 19 Puget Sound Terminal Nodes & 3-Letter WSF Codes
            terminals.forEach { terminal ->
                val pt = projectToCanvas(terminal.latitude, terminal.longitude, w, h)
                val isSelected = terminal.code == selectedTerminalCode
                val hasActiveStop = pendingStops.any {
                    it.originTerminalCode == terminal.code || it.destinationTerminalCode == terminal.code
                }

                val nodeColor = when {
                    isSelected -> BeaconAmberHigh
                    hasActiveStop -> WsfMarineTeal
                    else -> Color(0xFF64748B)
                }

                if (isSelected || hasActiveStop) {
                    drawCircle(
                        color = nodeColor.copy(alpha = 0.28f),
                        radius = if (isSelected) 16f else 12f,
                        center = pt
                    )
                }

                drawCircle(
                    color = nodeColor,
                    radius = if (isSelected) 7.5f else 5.5f,
                    center = pt
                )

                val labelLayout = textMeasurer.measure(
                    text = terminal.code,
                    style = TextStyle(
                        fontFamily = RobotoMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isSelected) 11.sp else 9.sp,
                        color = if (isSelected) BeaconAmberHigh else Color(0xFFE2E8F0)
                    )
                )
                val offsetX = if (terminal.longitude < -122.48) {
                    (pt.x - labelLayout.size.width - 8f).coerceAtLeast(4f)
                } else {
                    (pt.x + 8f).coerceAtMost(w - labelLayout.size.width - 4f)
                }
                val offsetY = (pt.y - labelLayout.size.height / 2f).coerceIn(2f, h - labelLayout.size.height - 2f)

                drawText(
                    textLayoutResult = labelLayout,
                    topLeft = Offset(offsetX, offsetY)
                )
            }
        }

        // Top-left Radar Legend Overlay
        Text(
            text = "PUGET SOUND WSF RADAR • TAP ANY TERMINAL NODE",
            style = MaterialTheme.typography.labelMedium,
            color = RadarCyan,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color(0xCC061014), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

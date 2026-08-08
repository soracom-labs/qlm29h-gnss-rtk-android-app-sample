package jp.co.soracom.qlm29hrtk.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Live and past-session map shell. The actual MapLibre lifecycle stays in TrackMap. */
@Composable
fun MapScreen(state: MapUiState, actions: MapActions) {
    Box(Modifier.fillMaxSize()) {
        TrackMapCard(
            points = state.points,
            smartphonePoints = state.smartphonePoints,
            smartphoneVisible = state.smartphoneVisible,
            onSmartphoneVisibleChange = actions.onSmartphoneVisibleChange,
            follow = state.follow,
            onFollowChange = actions.onFollowChange,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            Modifier.align(Alignment.TopCenter).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (state.showingPastSession) {
                Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f), shape = MaterialTheme.shapes.small) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Past session · ${state.points.size} points", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = actions.onReturnToLive) { Text("Return to live") }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusPill("USB", state.usbState, state.usbState == "Connected")
                val internetConnected = state.networkType !in setOf("Unknown", "None", "Offline")
                StatusPill("Internet", state.networkType, internetConnected)
                val ntripValue = if (state.ntripState == "Connected" && state.rtcmState != "Receiving") "Waiting" else state.ntripState
                StatusPill("NTRIP", ntripValue, state.ntripState == "Connected" && state.rtcmState == "Receiving")
                val soracomOk = state.soracomEnabled && state.lastSoracomHttpStatus in 200..299
                val soracomValue = when {
                    !state.soracomEnabled -> "Off"
                    soracomOk -> "OK"
                    state.soracomState == "Sending" -> "Sending"
                    state.soracomFailureCount > 0 -> "Error"
                    else -> "Waiting"
                }
                StatusPill("SORACOM", soracomValue, soracomOk)
            }
            state.latestFix?.let { fix ->
                val fixIsLive = state.usbState == "Connected"
                Surface(
                    color = (if (fixIsLive) qualityColor(fix.quality) else Color(0xFF616161)).copy(alpha = 0.90f),
                    contentColor = if (fixIsLive && fix.quality == 5) Color.Black else Color.White,
                    shape = MaterialTheme.shapes.small,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        "${if (fixIsLive) "Current fix" else "Last fix"} · ${fix.qualityLabel}  ·  Sat ${fix.satellites ?: "-"}  ·  HDOP ${fix.hdop ?: "-"}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, connected: Boolean) {
    val transitional = value in setOf("Connecting", "Reconnecting", "Sending", "Waiting")
    val color = when {
        connected -> Color(0xFF2E7D32)
        transitional -> Color(0xFFF9A825)
        value in setOf("Error", "Auth Error", "TLS Error") -> Color(0xFFC62828)
        else -> Color(0xFF616161)
    }
    Surface(color = color.copy(alpha = 0.88f), contentColor = Color.White, shape = MaterialTheme.shapes.small) {
        val marker = when {
            connected -> "●"
            transitional -> "◐"
            else -> "○"
        }
        Text("$marker $label", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

private fun qualityColor(quality: Int) = when (quality) {
    1 -> Color(0xFFF44336)
    2 -> Color(0xFF2196F3)
    4 -> Color(0xFF4CAF50)
    5 -> Color(0xFFFFEB3B)
    6 -> Color(0xFFFF9800)
    else -> Color(0xFF808080)
}

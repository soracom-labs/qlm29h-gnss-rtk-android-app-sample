package jp.co.soracom.qlm29hrtk.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import jp.co.soracom.qlm29hrtk.nmea.NmeaType

@Composable
fun DiagnosticsCard(state: DiagnosticsUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
            Text("USB RX last: ${state.lastUsbReceivedAt ?: "-"}")
            Text("USB TX last: ${state.lastUsbTransmittedAt ?: "-"}")
            Text("Checksum errors: ${state.checksumErrors} · GGA parse errors: ${state.ggaParseErrors}")
            Text(
                "NTRIP reconnects: ${state.ntripReconnectCount}" +
                    if (state.ntripConsecutiveFailureCount > 0) {
                        " · consecutive: ${state.ntripConsecutiveFailureCount}" +
                            state.ntripNextRetryDelaySeconds?.let { " · next: ${it}s" }.orEmpty()
                    } else "",
            )
            Text("SORACOM success: ${state.soracomSuccessCount} · failed: ${state.soracomFailureCount}")
            if (state.communicationEvents.isNotEmpty()) {
                Text("Recent connection events", style = MaterialTheme.typography.titleSmall)
                state.communicationEvents.takeLast(10).asReversed().forEach { event ->
                    Text(
                        "${event.occurredAt} · ${event.message}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                NmeaType.entries.joinToString(" · ") { "${it.name} ${state.sentenceCounts[it] ?: 0}" },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

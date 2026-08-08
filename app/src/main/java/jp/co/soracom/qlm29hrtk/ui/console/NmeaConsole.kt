package jp.co.soracom.qlm29hrtk.ui.console

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import jp.co.soracom.qlm29hrtk.nmea.ConsoleDirection
import jp.co.soracom.qlm29hrtk.nmea.ConsoleEntry
import jp.co.soracom.qlm29hrtk.nmea.ConsoleLogExporter
import jp.co.soracom.qlm29hrtk.nmea.NmeaType

@Composable
fun NmeaConsoleCard(
    entries: List<ConsoleEntry>,
    checksumErrors: Int,
    onClear: () -> Unit,
    onShareHistorical: () -> Unit,
) {
    val context = LocalContext.current
    var paused by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<List<ConsoleEntry>>(emptyList()) }
    var autoScroll by remember { mutableStateOf(true) }
    var showTimestamp by remember { mutableStateOf(true) }
    var showRx by remember { mutableStateOf(true) }
    var showTx by remember { mutableStateOf(true) }
    var invalidOnly by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var enabledTypes by remember { mutableStateOf(NmeaType.entries.toSet()) }
    var fileStatus by remember { mutableStateOf("") }
    var showShareOptions by remember { mutableStateOf(false) }
    val source = if (paused) frozen else entries
    val filtered = source.filter { entry ->
        (entry.direction == ConsoleDirection.RX && showRx || entry.direction == ConsoleDirection.TX && showTx) &&
            entry.type in enabledTypes && (!invalidOnly || !entry.checksumValid) &&
            (query.isBlank() || entry.text.contains(query, ignoreCase = true))
    }
    val listState = rememberLazyListState()
    LaunchedEffect(filtered.size, autoScroll, paused) {
        if (autoScroll && !paused && filtered.isNotEmpty()) listState.scrollToItem(filtered.lastIndex)
    }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("Share log") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showShareOptions = false
                            val file = ConsoleLogExporter.save(context, entries)
                            ConsoleLogExporter.share(context, file)
                            fileStatus = file.name
                        },
                    ) { Text("Share current log") }
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showShareOptions = false
                            onShareHistorical()
                        },
                    ) { Text("Share historical log") }
                    Text(
                        "Historical logs are selected by USB session in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showShareOptions = false }) { Text("Cancel") } },
        )
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("NMEA Console (${entries.size}/10,000)", style = MaterialTheme.typography.titleMedium)
                Text("Checksum errors: $checksumErrors", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (!paused) frozen = entries
                    paused = !paused
                }) { Text(if (paused) "Resume" else "Pause") }
                Button(onClick = { onClear(); frozen = emptyList() }) { Text("Clear") }
                Button(onClick = {
                    val file = ConsoleLogExporter.save(context, entries)
                    fileStatus = file.name
                }) { Text("Save") }
                Button(onClick = { showShareOptions = true }) { Text("Share") }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckOption("Auto scroll", autoScroll) { autoScroll = it }
                CheckOption("Timestamp", showTimestamp) { showTimestamp = it }
                CheckOption("RX", showRx) { showRx = it }
                CheckOption("TX", showTx) { showTx = it }
                CheckOption("Errors only", invalidOnly) { invalidOnly = it }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                NmeaType.entries.forEach { type ->
                    FilterChip(
                        selected = type in enabledTypes,
                        onClick = {
                            enabledTypes = if (type in enabledTypes) enabledTypes - type else enabledTypes + type
                        },
                        label = { Text(type.name) },
                    )
                }
            }
            if (fileStatus.isNotBlank()) Text("Saved: $fileStatus", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.fillMaxWidth().height(360.dp), state = listState) {
                items(filtered) { entry ->
                    val prefix = buildString {
                        if (showTimestamp) append(entry.timestamp).append(' ')
                        append(entry.direction.name).append(' ')
                        if (!entry.checksumValid) append("[CHECKSUM ERROR] ")
                    }
                    Text(prefix + entry.text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CheckOption(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

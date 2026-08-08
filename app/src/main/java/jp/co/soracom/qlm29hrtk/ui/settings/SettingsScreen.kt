package jp.co.soracom.qlm29hrtk.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import jp.co.soracom.qlm29hrtk.soracom.SoracomQualityPolicy
import jp.co.soracom.qlm29hrtk.soracom.SoracomSchedulePolicy
import jp.co.soracom.qlm29hrtk.sessionlog.NmeaExportSource
import jp.co.soracom.qlm29hrtk.sessionlog.SessionLogShare
import jp.co.soracom.qlm29hrtk.storage.StorageInspector
import jp.co.soracom.qlm29hrtk.ui.map.MapCacheControls
import jp.co.soracom.qlm29hrtk.usb.UsbPermissionManager
import java.time.Instant

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    permissionManager: UsbPermissionManager,
    onSmartphoneGnssToggle: (Boolean) -> Unit,
    sessionsNavigationRequest: Int = 0,
) {
    val listState = rememberLazyListState()
    val sessionsIndex = if (state.error == null) 7 else 8
    LaunchedEffect(sessionsNavigationRequest, sessionsIndex) {
        if (sessionsNavigationRequest > 0) listState.animateScrollToItem(sessionsIndex)
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let { message ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("Error: $message", Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Display", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dark theme")
                        Checkbox(checked = state.display.darkTheme, onCheckedChange = {
                            actions.updateDarkTheme(it)
                            actions.saveNtripSettings()
                        })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Keep screen on")
                            Text("Prevent screen timeout while this app is visible", style = MaterialTheme.typography.bodySmall)
                        }
                        Checkbox(checked = state.display.keepScreenOn, onCheckedChange = actions::updateKeepScreenOn)
                    }
                }
            }
        }
        item { SmartphoneGnssControls(state.smartphone, actions, onSmartphoneGnssToggle) }
        item { UsbControls(
            state = state.usb,
            onSelect = actions::selectDevice,
            onRefresh = actions::refreshDevices,
            onAutoConnectChange = actions::updateUsbAutoConnect,
            onConnect = {
                val id = state.usb.selectedDeviceId ?: run {
                    actions.showError("Select a USB device before connecting")
                    return@UsbControls
                }
                if (permissionManager.hasPermission(id)) actions.connect(id) else permissionManager.request(id)
            },
            onDisconnect = actions::disconnect,
        ) }
        item { NtripControls(state.ntrip, actions) }
        item { SoracomControls(state.soracom, actions) }
        item { TrackControls(state.storage, actions) }
        item { MapCacheControls() }
        item { SessionHistoryCard(state.sessions, actions) }
        item { StorageCard(state.storage) }
        item { DiagnosticsCard(state.diagnostics) }
    }
}

@androidx.compose.runtime.Composable
private fun SmartphoneGnssControls(state: SmartphoneSettingsUiState, actions: SettingsActions, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Smartphone GNSS", style = MaterialTheme.typography.titleMedium)
                ConnectionSwitch(
                    checked = state.enabled,
                    busy = state.status == "Starting",
                    offLabel = "Disabled",
                    onLabel = "Enabled",
                    onCheckedChange = onToggle,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.background, onCheckedChange = actions::updateSmartphoneGnssBackground)
                Column {
                    Text("Continue in background")
                    Text("Keep recording while the screen is off or another app is open", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Status: ${state.status} · Provider: GPS")
            Text("Last: ${state.lastLocationAt ?: "-"} · Accuracy: ${state.accuracy?.let { "±${"%.1f".format(it)} m" } ?: "-"}")
            Text("Saved SP points: ${state.pointCount} / 50,000", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@androidx.compose.runtime.Composable
private fun SessionHistoryCard(state: SessionsUiState, actions: SettingsActions) {
    val context = LocalContext.current
    var sessionToDelete by remember { mutableStateOf<jp.co.soracom.qlm29hrtk.storage.SessionEntity?>(null) }
    var exportingSessionId by remember { mutableStateOf<String?>(null) }
    var exportMessage by remember { mutableStateOf("") }
    var showAllSessions by remember { mutableStateOf(false) }
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete session?") },
            text = {
                Text(
                    "The session started at ${Instant.ofEpochMilli(session.startedAt)} and its ${session.pointCount} track points will be deleted. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    sessionToDelete = null
                    actions.deleteSession(session.id)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") } },
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Sessions", style = MaterialTheme.typography.titleMedium)
                if (state.selectedMapSessionId != null) {
                    TextButton(onClick = { actions.selectMapSession(null) }) { Text("Show live") }
                }
            }
            if (state.sessions.isEmpty()) Text("No sessions")
            if (exportMessage.isNotBlank()) Text(exportMessage, style = MaterialTheme.typography.bodySmall)
            val visibleSessions = if (showAllSessions) state.sessions else state.sessions.take(10)
            visibleSessions.forEach { session ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Instant.ofEpochMilli(session.startedAt).toString())
                        Text(
                            "${session.pointCount} points · Fixed ${session.rtkFixedCount} · Float ${session.rtkFloatCount} · SPS ${session.spsCount}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(if (session.endedAt == null) "Active" else "Ended: ${Instant.ofEpochMilli(session.endedAt).toString()}", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (session.endedAt != null) {
                            TextButton(
                                onClick = { actions.selectMapSession(session.id) },
                                enabled = state.selectedMapSessionId != session.id,
                            ) { Text(if (state.selectedMapSessionId == session.id) "Showing" else "Show on Map") }
                        }
                        TextButton(
                            onClick = {
                                exportingSessionId = session.id
                                actions.exportSessionLogs(session.id) { result ->
                                    exportingSessionId = null
                                    result.onSuccess { export ->
                                        runCatching { SessionLogShare.share(context, export) }
                                            .onFailure { actions.showError(it.message ?: "Unable to share session log") }
                                        exportMessage = when (export.nmeaSource) {
                                            NmeaExportSource.RAW_SESSION -> "Full NMEA session prepared"
                                            NmeaExportSource.GGA_FALLBACK -> "GGA-only replay prepared for this older session"
                                        }
                                    }
                                }
                            },
                            enabled = session.endedAt != null && exportingSessionId == null,
                        ) { Text(if (exportingSessionId == session.id) "Preparing…" else "Share logs") }
                        TextButton(onClick = { sessionToDelete = session }, enabled = session.endedAt != null) { Text("Delete") }
                    }
                }
            }
            if (state.sessions.size > 10) {
                TextButton(onClick = { showAllSessions = !showAllSessions }) {
                    Text(if (showAllSessions) "Show latest 10" else "Show all ${state.sessions.size} sessions")
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StorageCard(state: TrackStorageUiState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var logBytes by remember { mutableStateOf(StorageInspector.nmeaLogBytes(context)) }
    var message by remember { mutableStateOf("") }
    var confirmDeleteLogs by remember { mutableStateOf(false) }
    if (confirmDeleteLogs) {
        AlertDialog(
            onDismissRequest = { confirmDeleteLogs = false },
            title = { Text("Delete NMEA logs?") },
            text = { Text("All saved NMEA log files (${StorageInspector.formatBytes(logBytes)}) will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteLogs = false
                    val deleted = StorageInspector.clearNmeaLogs(context)
                    logBytes = StorageInspector.nmeaLogBytes(context)
                    message = "$deleted log files deleted"
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteLogs = false }) { Text("Cancel") } },
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Storage", style = MaterialTheme.typography.titleMedium)
            Text("Track points: ${state.qlmPointCount} / 50,000 · Retention: 7 days")
            Text("NMEA logs: ${StorageInspector.formatBytes(logBytes)}")
            Button(
                onClick = { confirmDeleteLogs = true },
                enabled = !state.sessionLogCaptureActive,
            ) { Text("Delete NMEA logs") }
            if (state.sessionLogCaptureActive) {
                Text("Disconnect USB before deleting session logs", style = MaterialTheme.typography.bodySmall)
            }
            if (message.isNotBlank()) Text(message)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@androidx.compose.runtime.Composable
private fun SoracomControls(state: SoracomSettingsUiState, actions: SettingsActions) {
    var confirmTestSend by remember { mutableStateOf(false) }
    var pendingIntervalSeconds by remember { mutableStateOf<Int?>(null) }
    if (confirmTestSend) {
        AlertDialog(
            onDismissRequest = { confirmTestSend = false },
            title = { Text("Send latest fix?") },
            text = { Text("The latest GNSS fix will be posted once to http://uni.soracom.io. Periodic POST will remain ${if (state.enabled) "enabled" else "disabled"}.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmTestSend = false
                    actions.sendSoracomNow()
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { confirmTestSend = false }) { Text("Cancel") } },
        )
    }
    pendingIntervalSeconds?.let { seconds ->
        AlertDialog(
            onDismissRequest = { pendingIntervalSeconds = null },
            title = { Text("Confirm high-frequency POST") },
            text = {
                Text(
                    "A $seconds-second interval may cause unexpected SORACOM or mobile data charges. " +
                        "Have you estimated the request volume and cost?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingIntervalSeconds = null
                        actions.updateSoracomInterval(seconds.toString())
                    },
                ) { Text("Use $seconds sec") }
            },
            dismissButton = {
                TextButton(onClick = { pendingIntervalSeconds = null }) { Text("Cancel") }
            },
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("SORACOM Unified Endpoint", style = MaterialTheme.typography.titleMedium)
                ConnectionSwitch(
                    checked = state.enabled,
                    busy = state.status in setOf("Validating", "Sending"),
                    offLabel = "Disabled",
                    onLabel = "Enabled",
                    onCheckedChange = actions::updateSoracomEnabled,
                )
            }
            Text("Endpoint: http://uni.soracom.io · ${state.status} · Retry: Disabled")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("POST interval")
                androidx.compose.material3.FilterChip(
                    selected = state.intervalSeconds.toIntOrNull() == SoracomSchedulePolicy.DEFAULT_INTERVAL_SECONDS,
                    onClick = {
                        if (state.intervalSeconds.toIntOrNull() != SoracomSchedulePolicy.DEFAULT_INTERVAL_SECONDS) {
                            actions.updateSoracomInterval(SoracomSchedulePolicy.DEFAULT_INTERVAL_SECONDS.toString())
                        }
                    },
                    label = { Text("default 60s") },
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SoracomSchedulePolicy.HIGH_FREQUENCY_INTERVAL_SECONDS.forEach { seconds ->
                    androidx.compose.material3.FilterChip(
                        selected = state.intervalSeconds.toIntOrNull() == seconds,
                        onClick = {
                            if (state.intervalSeconds.toIntOrNull() != seconds) {
                                if (SoracomSchedulePolicy.requiresCostConfirmation(seconds)) {
                                    pendingIntervalSeconds = seconds
                                } else {
                                    actions.updateSoracomInterval(seconds.toString())
                                }
                            }
                        },
                        label = { Text("${seconds}s") },
                    )
                }
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = state.sendNoFix, onCheckedChange = actions::updateSoracomSendNoFix)
                Text("Send No Fix")
                Checkbox(checked = state.allowNtripDisconnected, onCheckedChange = actions::updateSoracomAllowNtripDisconnected)
                Text("Allow NTRIP disconnected")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SoracomQualityPolicy.entries.forEach { policy ->
                    androidx.compose.material3.FilterChip(
                        selected = state.qualityPolicy == policy,
                        onClick = { actions.updateSoracomQualityPolicy(policy) },
                        label = { Text(when (policy) {
                            SoracomQualityPolicy.ALL_VALID -> "All valid"
                            SoracomQualityPolicy.RTK_FLOAT_OR_BETTER -> "Float+"
                            SoracomQualityPolicy.RTK_FIXED_ONLY -> "Fixed only"
                        }) },
                    )
                }
            }
            Text("Network: ${state.networkType}  HTTP: ${state.lastHttpStatus ?: "-"}  Failures: ${state.failureCount}")
            Text("Last attempt: ${state.lastSentAt ?: "-"}")
            Button(
                onClick = { confirmTestSend = true },
                enabled = state.latestFix != null && state.status !in setOf("Sending", "Validating"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1FAEB8),
                    contentColor = Color.White,
                ),
            ) { Text("Test send latest fix") }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TrackControls(state: TrackStorageUiState, actions: SettingsActions) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear track cache?") },
            text = { Text("All QLM29H and Smartphone GNSS track points and session history will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    actions.clearTracks()
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Column {
                Text("Track cache", style = MaterialTheme.typography.titleMedium)
                Text("${state.qlmPointCount} / 50,000 points · 7 days")
                Text("SP: ${state.smartphonePointCount} / 50,000 points · 7 days", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { confirmClear = true }) { Text("Clear") }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NtripControls(state: NtripSettingsUiState, actions: SettingsActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("NTRIP", style = MaterialTheme.typography.titleMedium)
                ConnectionSwitch(
                    checked = state.connection == "Connected",
                    busy = state.connection in setOf("Connecting", "Reconnecting"),
                    offLabel = "Disconnect",
                    onLabel = "Connect",
                    onCheckedChange = { if (it) actions.connectNtrip() else actions.disconnectNtrip() },
                )
            }
            OutlinedTextField(
                value = state.host,
                onValueChange = actions::updateNtripHost,
                label = { Text("Host") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.port,
                    onValueChange = actions::updateNtripPort,
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.mountPoint,
                    onValueChange = actions::updateNtripMountPoint,
                    label = { Text("Mount Point") },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.username,
                    onValueChange = actions::updateNtripUsername,
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = actions::updateNtripPassword,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.weight(1f),
                )
            }
            Text("RTCM: ${state.rtcmBytes} bytes  ${state.lastRtcmMessage ?: "No messages"}")
            Text("RTCM state: ${state.rtcmState}  Last: ${state.lastRtcmReceivedAt ?: "-"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions::saveNtripSettings, enabled = state.settingsState != "Saving") { Text("Save") }
                Button(onClick = actions::fetchSourceTable, enabled = state.sourceTableState != "Loading") { Text("Source Table") }
            }
            if (state.settingsState.isNotBlank()) Text("Settings: ${state.settingsState}")
            Text("Source Table: ${state.sourceTableState}")
            state.mountPoints.take(20).forEach { point ->
                val marker = if (state.mountPoint.trimStart('/') == point.name) "●" else "○"
                Text(
                    "$marker ${point.name}  ${point.format ?: "Unknown"}  ${point.latitude ?: "-"}, ${point.longitude ?: "-"}",
                    modifier = Modifier.fillMaxWidth().clickable { actions.chooseMountPoint(point.name) }.padding(vertical = 3.dp),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun UsbControls(
    state: UsbSettingsUiState,
    onSelect: (Int) -> Unit,
    onRefresh: () -> Unit,
    onAutoConnectChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("USB devices", style = MaterialTheme.typography.titleMedium)
                ConnectionSwitch(
                    checked = state.connection == "Connected",
                    busy = state.connection == "Connecting",
                    offLabel = "Disconnect",
                    onLabel = "Connect",
                    onCheckedChange = { if (it) onConnect() else onDisconnect() },
                )
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = state.autoConnect, onCheckedChange = onAutoConnectChange)
                Text("Auto reconnect when permission is already granted")
            }
            if (state.devices.isEmpty()) Text("No compatible serial device")
            state.devices.forEach { device ->
                val marker = if (state.selectedDeviceId == device.id) "●" else "○"
                Text(
                    "$marker ${device.name}  VID:%04X PID:%04X  ${device.driver}".format(device.vendorId, device.productId),
                    modifier = Modifier.fillMaxWidth().clickable { onSelect(device.id) }.padding(vertical = 4.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("Refresh") }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ConnectionSwitch(
    checked: Boolean,
    busy: Boolean,
    offLabel: String,
    onLabel: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(if (busy) "Checking…" else if (checked) onLabel else offLabel, style = MaterialTheme.typography.labelSmall)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = !busy,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF8A8A8A),
                uncheckedThumbColor = Color.White,
                disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                disabledUncheckedTrackColor = Color(0xFF8A8A8A).copy(alpha = 0.55f),
            ),
        )
    }
}

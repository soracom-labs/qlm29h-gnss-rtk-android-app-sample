package jp.co.soracom.qlm29hrtk.ui.settings

import jp.co.soracom.qlm29hrtk.AppState
import jp.co.soracom.qlm29hrtk.nmea.GgaFix
import jp.co.soracom.qlm29hrtk.nmea.NmeaType
import jp.co.soracom.qlm29hrtk.ntrip.MountPoint
import jp.co.soracom.qlm29hrtk.soracom.SoracomQualityPolicy
import jp.co.soracom.qlm29hrtk.storage.SessionEntity
import jp.co.soracom.qlm29hrtk.usb.UsbSerialDevice
import jp.co.soracom.qlm29hrtk.UsbConnectionState

data class DisplaySettingsUiState(val darkTheme: Boolean, val keepScreenOn: Boolean)
data class SmartphoneSettingsUiState(
    val enabled: Boolean,
    val background: Boolean,
    val status: String,
    val lastLocationAt: String?,
    val accuracy: Float?,
    val pointCount: Int,
)
data class UsbSettingsUiState(
    val connection: String,
    val autoConnect: Boolean,
    val devices: List<UsbSerialDevice>,
    val selectedDeviceId: Int?,
)
data class NtripSettingsUiState(
    val host: String,
    val port: String,
    val mountPoint: String,
    val username: String,
    val password: String,
    val connection: String,
    val rtcmBytes: Long,
    val lastRtcmMessage: String?,
    val rtcmState: String,
    val lastRtcmReceivedAt: String?,
    val settingsState: String,
    val sourceTableState: String,
    val mountPoints: List<MountPoint>,
)
data class SoracomSettingsUiState(
    val enabled: Boolean,
    val intervalSeconds: String,
    val status: String,
    val sendNoFix: Boolean,
    val allowNtripDisconnected: Boolean,
    val qualityPolicy: SoracomQualityPolicy,
    val networkType: String,
    val lastHttpStatus: Int?,
    val failureCount: Int,
    val lastSentAt: String?,
    val latestFix: GgaFix?,
)
data class TrackStorageUiState(
    val qlmPointCount: Int,
    val smartphonePointCount: Int,
    val pointLimit: Int,
    val sessionLogCaptureActive: Boolean,
)
data class SessionsUiState(val sessions: List<SessionEntity>, val selectedMapSessionId: String?)
data class DiagnosticsUiState(
    val lastUsbReceivedAt: String?,
    val lastUsbTransmittedAt: String?,
    val checksumErrors: Int,
    val ggaParseErrors: Int,
    val ntripReconnectCount: Int,
    val soracomSuccessCount: Int,
    val soracomFailureCount: Int,
    val sentenceCounts: Map<NmeaType, Long>,
)

data class SettingsUiState(
    val error: String?,
    val display: DisplaySettingsUiState,
    val smartphone: SmartphoneSettingsUiState,
    val usb: UsbSettingsUiState,
    val ntrip: NtripSettingsUiState,
    val soracom: SoracomSettingsUiState,
    val storage: TrackStorageUiState,
    val sessions: SessionsUiState,
    val diagnostics: DiagnosticsUiState,
) {
    companion object {
        fun from(state: AppState) = SettingsUiState(
            error = state.notice.error,
            display = DisplaySettingsUiState(state.display.darkTheme, state.display.keepScreenOn),
            smartphone = SmartphoneSettingsUiState(
                state.smartphone.enabled, state.smartphone.background, state.smartphone.status.label,
                state.smartphone.lastLocationAt, state.smartphone.accuracy, state.smartphone.pointCount,
            ),
            usb = UsbSettingsUiState(state.usb.connection.label, state.usb.autoConnect, state.usb.devices, state.usb.selectedDeviceId),
            ntrip = NtripSettingsUiState(
                state.ntrip.host, state.ntrip.port, state.ntrip.mountPoint, state.ntrip.username, state.ntrip.password,
                state.ntrip.connection.label, state.ntrip.rtcmBytes, state.ntrip.lastRtcmMessage, state.ntrip.rtcmState.label,
                state.ntrip.lastRtcmReceivedAt, state.ntrip.settingsState.label, state.ntrip.sourceTableState.label, state.ntrip.mountPoints,
            ),
            soracom = SoracomSettingsUiState(
                state.soracom.enabled, state.soracom.intervalSeconds, state.soracom.status.label, state.soracom.sendNoFix,
                state.soracom.allowNtripDisconnected, state.soracom.qualityPolicy, state.soracom.networkType,
                state.soracom.lastHttpStatus, state.soracom.failureCount, state.soracom.lastSentAt, state.tracking.latestFix,
            ),
            storage = TrackStorageUiState(
                state.tracking.pointCount,
                state.smartphone.pointCount,
                state.storage.trackPointLimit,
                state.usb.connection == UsbConnectionState.CONNECTED,
            ),
            sessions = SessionsUiState(state.tracking.sessions, state.tracking.selectedSessionId),
            diagnostics = DiagnosticsUiState(
                state.usb.lastReceivedAt, state.usb.lastTransmittedAt,
                state.diagnostics.checksumErrors, state.diagnostics.ggaParseErrors,
                state.ntrip.reconnectCount, state.soracom.successCount, state.soracom.failureCount,
                state.diagnostics.sentenceCounts,
            ),
        )
    }
}

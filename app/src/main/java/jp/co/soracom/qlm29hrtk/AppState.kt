package jp.co.soracom.qlm29hrtk

import jp.co.soracom.qlm29hrtk.nmea.ConsoleEntry
import jp.co.soracom.qlm29hrtk.nmea.GgaFix
import jp.co.soracom.qlm29hrtk.nmea.NmeaType
import jp.co.soracom.qlm29hrtk.ntrip.MountPoint
import jp.co.soracom.qlm29hrtk.ntrip.NtripDefaults
import jp.co.soracom.qlm29hrtk.network.InternetReachability
import jp.co.soracom.qlm29hrtk.location.TrackRetentionPolicy
import jp.co.soracom.qlm29hrtk.soracom.SoracomQualityPolicy
import jp.co.soracom.qlm29hrtk.storage.SessionEntity
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import jp.co.soracom.qlm29hrtk.usb.UsbSerialDevice

/** Display preferences change together and do not belong to an I/O session. */
data class AppDisplayState(
    val darkTheme: Boolean = false,
    val keepScreenOn: Boolean = false,
)

/** Shared storage policy for QLM and SP points; point counts remain in their owning feature states. */
data class AppStorageState(
    val trackPointLimit: Int = TrackRetentionPolicy.DEFAULT_MAX_POINTS,
)

/** Default-network reachability is independent from SORACOM publication state. */
data class AppConnectivityState(
    val internet: InternetReachability = InternetReachability.OFFLINE,
)

enum class UsbConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    ERROR("Error"),
    ;

    val isActive: Boolean get() = this == CONNECTING || this == CONNECTED
}

/** USB connection, selection and transport counters share one session boundary. */
data class AppUsbState(
    val connection: UsbConnectionState = UsbConnectionState.DISCONNECTED,
    val devices: List<UsbSerialDevice> = emptyList(),
    val selectedDeviceId: Int? = null,
    val receivedBytes: Long = 0,
    val transmittedBytes: Long = 0,
    val autoConnect: Boolean = true,
    val lastReceivedAt: String? = null,
    val lastTransmittedAt: String? = null,
)

enum class NtripConnectionState(val label: String) {
    DISCONNECTED("Disconnected"),
    WAITING_FOR_GGA("Waiting for GGA"),
    CONNECTING("Connecting"),
    CONNECTED("Connected"),
    RECONNECTING("Reconnecting"),
    AUTH_ERROR("Auth Error"),
    TLS_ERROR("TLS Error"),
    CONFIGURATION_ERROR("Config Error"),
    ERROR("Error"),
    ;

    val isConnected: Boolean get() = this == CONNECTED
    val hasActiveSession: Boolean get() = this == WAITING_FOR_GGA || this == CONNECTING || this == CONNECTED || this == RECONNECTING
}

enum class RtcmStreamState(val label: String) {
    NONE("None"),
    RECEIVING("Receiving"),
    STALE("Stale"),
}

enum class SettingsPersistenceState(val label: String) {
    IDLE(""),
    SAVING("Saving"),
    SAVED("Saved"),
    ERROR("Error"),
}

sealed class SourceTableStatus(val label: String) {
    data object Idle : SourceTableStatus("Idle")
    data object Loading : SourceTableStatus("Loading")
    data object Error : SourceTableStatus("Error")
    data class Loaded(val count: Int) : SourceTableStatus("$count mount points")
}

/** NTRIP configuration, session status and the resulting RTCM stream. */
data class AppNtripState(
    val host: String = NtripDefaults.HOST,
    val port: String = NtripDefaults.PORT.toString(),
    val mountPoint: String = NtripDefaults.MOUNT_POINT,
    val username: String = "",
    val password: String = "",
    val connection: NtripConnectionState = NtripConnectionState.DISCONNECTED,
    val rtcmBytes: Long = 0,
    val lastRtcmMessage: String? = null,
    val rtcmState: RtcmStreamState = RtcmStreamState.NONE,
    val lastRtcmReceivedAt: String? = null,
    val mountPoints: List<MountPoint> = emptyList(),
    val sourceTableState: SourceTableStatus = SourceTableStatus.Idle,
    val settingsState: SettingsPersistenceState = SettingsPersistenceState.IDLE,
    val reconnectCount: Int = 0,
    val consecutiveFailureCount: Int = 0,
    val nextRetryDelaySeconds: Long? = null,
)

enum class SoracomPublicationState(val label: String) {
    DISABLED("Disabled"),
    VALIDATING("Validating"),
    SUCCESS("Success"),
    IDLE("Idle"),
    SENDING("Sending"),
    FAILED("Failed"),
    ;

    val isBusy: Boolean get() = this == VALIDATING || this == SENDING
}

/** SORACOM publication configuration, policy and delivery diagnostics. */
data class AppSoracomState(
    val enabled: Boolean = false,
    val intervalSeconds: String = "60",
    val status: SoracomPublicationState = SoracomPublicationState.DISABLED,
    val lastSentAt: String? = null,
    val lastHttpStatus: Int? = null,
    val failureCount: Int = 0,
    val successCount: Int = 0,
    val networkType: String = "Unknown",
    val sendNoFix: Boolean = false,
    val allowNtripDisconnected: Boolean = true,
    val qualityPolicy: SoracomQualityPolicy = SoracomQualityPolicy.ALL_VALID,
)

enum class SmartphoneGnssStatus(val label: String) {
    DISABLED("Disabled"),
    PERMISSION_REQUIRED("Permission required"),
    GPS_DISABLED("GPS disabled"),
    STARTING("Starting"),
    RECORDING("Recording"),
    WAITING_FOR_GPS("Waiting for GPS"),
    PAUSED("Paused"),
    ERROR("Error"),
}

/** Smartphone-only location capture; never feeds NTRIP, SORACOM or Console. */
data class AppSmartphoneState(
    val enabled: Boolean = false,
    val background: Boolean = false,
    val trackVisible: Boolean = true,
    val status: SmartphoneGnssStatus = SmartphoneGnssStatus.DISABLED,
    val points: List<SmartphoneTrackPointEntity> = emptyList(),
    val selectedSessionPoints: List<SmartphoneTrackPointEntity> = emptyList(),
    val selectedSessionPointsLoaded: Boolean = false,
    val pointCount: Int = 0,
    val lastLocationAt: String? = null,
    val accuracy: Float? = null,
)

/** QLM-derived fix, persisted track and the user's current map session view. */
data class AppTrackingState(
    val latestFix: GgaFix? = null,
    val pointCount: Int = 0,
    val livePoints: List<TrackPointEntity> = emptyList(),
    val sessions: List<SessionEntity> = emptyList(),
    val selectedSessionId: String? = null,
    val selectedSessionPoints: List<TrackPointEntity> = emptyList(),
    val follow: Boolean = true,
)

/** Non-sensitive connectivity history; never include endpoints, credentials or coordinates. */
data class AppCommunicationEvent(
    val occurredAt: String,
    val message: String,
)

/** Bounded raw NMEA console, parser diagnostics and safe connection events. */
data class AppDiagnosticsState(
    val console: List<ConsoleEntry> = emptyList(),
    val checksumErrors: Int = 0,
    val ggaParseErrors: Int = 0,
    val sentenceCounts: Map<NmeaType, Long> = emptyMap(),
    val communicationEvents: List<AppCommunicationEvent> = emptyList(),
)

/** Transient user-facing notice, kept outside every protocol state. */
data class AppNoticeState(val error: String? = null)

/** UI-readable snapshot produced by the application-scoped RTK runtime. */
data class AppState(
    val usb: AppUsbState = AppUsbState(),
    val tracking: AppTrackingState = AppTrackingState(),
    val diagnostics: AppDiagnosticsState = AppDiagnosticsState(),
    val notice: AppNoticeState = AppNoticeState(),
    val ntrip: AppNtripState = AppNtripState(),
    val soracom: AppSoracomState = AppSoracomState(),
    val display: AppDisplayState = AppDisplayState(),
    val storage: AppStorageState = AppStorageState(),
    val smartphone: AppSmartphoneState = AppSmartphoneState(),
    val connectivity: AppConnectivityState = AppConnectivityState(),
)

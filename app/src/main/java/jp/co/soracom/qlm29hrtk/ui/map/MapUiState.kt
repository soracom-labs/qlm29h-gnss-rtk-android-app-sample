package jp.co.soracom.qlm29hrtk.ui.map

import jp.co.soracom.qlm29hrtk.AppState
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import jp.co.soracom.qlm29hrtk.nmea.GgaFix
import jp.co.soracom.qlm29hrtk.network.InternetReachability

data class MapUiState(
    val points: List<TrackPointEntity>,
    val smartphonePoints: List<SmartphoneTrackPointEntity>,
    val smartphoneVisible: Boolean,
    val showingPastSession: Boolean,
    val historicalSmartphoneStatus: String?,
    val follow: Boolean,
    val usbState: String,
    val internet: InternetReachability,
    val ntripState: String,
    val rtcmState: String,
    val soracomEnabled: Boolean,
    val soracomState: String,
    val soracomFailureCount: Int,
    val lastSoracomHttpStatus: Int?,
    val latestFix: GgaFix?,
) {
    companion object {
        /** MAP-03/SP-06: past mode combines only session-associated QLM and SP data. */
        fun from(state: AppState): MapUiState {
            val past = state.tracking.selectedSessionId != null
            return MapUiState(
                points = if (past) state.tracking.selectedSessionPoints else state.tracking.livePoints,
                smartphonePoints = if (past) state.smartphone.selectedSessionPoints else state.smartphone.points,
                smartphoneVisible = state.smartphone.trackVisible,
                showingPastSession = past,
                historicalSmartphoneStatus = if (!past) null else when {
                    !state.smartphone.selectedSessionPointsLoaded -> "SP loading…"
                    state.smartphone.selectedSessionPoints.isEmpty() -> "SP 0 points (not recorded or no longer retained)"
                    else -> "SP ${state.smartphone.selectedSessionPoints.size} points"
                },
                follow = state.tracking.follow,
                usbState = state.usb.connection.label,
                internet = state.connectivity.internet,
                ntripState = state.ntrip.connection.label,
                rtcmState = state.ntrip.rtcmState.label,
                soracomEnabled = state.soracom.enabled,
                soracomState = state.soracom.status.label,
                soracomFailureCount = state.soracom.failureCount,
                lastSoracomHttpStatus = state.soracom.lastHttpStatus,
                latestFix = state.tracking.latestFix,
            )
        }
    }
}

data class MapActions(
    val onSmartphoneVisibleChange: (Boolean) -> Unit,
    val onFollowChange: (Boolean) -> Unit,
    val onReturnToLive: () -> Unit,
)

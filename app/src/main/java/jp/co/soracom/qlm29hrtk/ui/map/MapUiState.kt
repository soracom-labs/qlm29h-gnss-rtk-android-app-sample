package jp.co.soracom.qlm29hrtk.ui.map

import jp.co.soracom.qlm29hrtk.AppState
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import jp.co.soracom.qlm29hrtk.nmea.GgaFix

data class MapUiState(
    val points: List<TrackPointEntity>,
    val smartphonePoints: List<SmartphoneTrackPointEntity>,
    val smartphoneVisible: Boolean,
    val showingPastSession: Boolean,
    val follow: Boolean,
    val usbState: String,
    val networkType: String,
    val ntripState: String,
    val rtcmState: String,
    val soracomEnabled: Boolean,
    val soracomState: String,
    val soracomFailureCount: Int,
    val lastSoracomHttpStatus: Int?,
    val latestFix: GgaFix?,
) {
    companion object {
        /** MAP-03: past mode is a stable QLM-only projection of runtime state. */
        fun from(state: AppState): MapUiState {
            val past = state.tracking.selectedSessionId != null
            return MapUiState(
                points = if (past) state.tracking.selectedSessionPoints else state.tracking.livePoints,
                smartphonePoints = if (past) emptyList() else state.smartphone.points,
                smartphoneVisible = !past && state.smartphone.trackVisible,
                showingPastSession = past,
                follow = state.tracking.follow,
                usbState = state.usb.connection.label,
                networkType = state.soracom.networkType,
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

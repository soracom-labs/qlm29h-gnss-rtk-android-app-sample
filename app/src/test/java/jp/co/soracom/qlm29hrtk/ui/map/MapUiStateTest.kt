package jp.co.soracom.qlm29hrtk.ui.map

import jp.co.soracom.qlm29hrtk.AppState
import jp.co.soracom.qlm29hrtk.AppSmartphoneState
import jp.co.soracom.qlm29hrtk.AppTrackingState
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapUiStateTest {
    @Test fun liveProjectionIncludesQlmAndVisibleSmartphoneTracks() {
        val qlm = qlmPoint(1)
        val sp = smartphonePoint(2)
        val result = MapUiState.from(
            AppState(tracking = AppTrackingState(livePoints = listOf(qlm)), smartphone = AppSmartphoneState(points = listOf(sp))),
        )

        assertEquals(listOf(qlm), result.points)
        assertEquals(listOf(sp), result.smartphonePoints)
        assertTrue(result.smartphoneVisible)
        assertFalse(result.showingPastSession)
    }

    @Test fun pastProjectionUsesTheSelectedQlmSessionAndSuppressesSmartphoneTrack() {
        val past = qlmPoint(3)
        val result = MapUiState.from(
            AppState(
                tracking = AppTrackingState(
                    livePoints = listOf(qlmPoint(1)),
                    selectedSessionId = "past",
                    selectedSessionPoints = listOf(past),
                ),
                smartphone = AppSmartphoneState(points = listOf(smartphonePoint(2))),
            ),
        )

        assertEquals(listOf(past), result.points)
        assertTrue(result.smartphonePoints.isEmpty())
        assertFalse(result.smartphoneVisible)
        assertTrue(result.showingPastSession)
    }

    private fun qlmPoint(id: Long) = TrackPointEntity(
        id, "session", id, 35.0, 139.0, null, 4, "RTK Fixed", 20, 0.8, true, null, null,
    )

    private fun smartphonePoint(id: Long) = SmartphoneTrackPointEntity(
        id, "segment", id, 35.0, 139.0, null, 3f, 0f, null, "gps",
    )
}

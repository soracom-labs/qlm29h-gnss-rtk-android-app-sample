package jp.co.soracom.qlm29hrtk.ui.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewportPolicyTest {
    private val qlm = MapFollowTarget(MapFollowTarget.Source.QLM, 2_000, 35.0, 139.0)
    private val smartphone = MapFollowTarget(MapFollowTarget.Source.SMARTPHONE, 3_000, 35.1, 139.1)

    @Test fun qlmAlwaysWinsEvenWhenSmartphoneIsNewer() {
        assertEquals(qlm, MapViewportPolicy.chooseTarget(qlm, smartphone))
        assertEquals(smartphone, MapViewportPolicy.chooseTarget(null, smartphone))
    }

    @Test fun smartphoneEmissionCannotMoveAnEstablishedQlmCamera() {
        assertFalse(
            MapViewportPolicy.shouldMove(true, true, MapFollowTarget.Source.QLM, qlm.timestamp, qlm),
        )
    }

    @Test fun cameraMovesWhenFollowResumesOrAuthoritativeTimestampAdvances() {
        assertTrue(MapViewportPolicy.shouldMove(true, false, MapFollowTarget.Source.QLM, 2_000, qlm))
        assertTrue(MapViewportPolicy.shouldMove(true, true, MapFollowTarget.Source.QLM, 1_999, qlm))
        assertFalse(MapViewportPolicy.shouldMove(false, false, null, Long.MIN_VALUE, qlm))
    }
}

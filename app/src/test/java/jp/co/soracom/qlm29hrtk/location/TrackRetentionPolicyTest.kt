package jp.co.soracom.qlm29hrtk.location

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackRetentionPolicyTest {
    @Test fun keepsAtMostFiftyThousandPoints() {
        assertEquals(50_000, TrackRetentionPolicy.maxPoints)
    }

    @Test fun computesSevenDayCutoff() {
        val now = 1_000_000_000L
        assertEquals(now - 7L * 24 * 60 * 60 * 1_000, TrackRetentionPolicy.cutoff(now))
    }
}

package jp.co.soracom.qlm29hrtk.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackRetentionPolicyTest {
    @Test fun samplingUsesGgaUtcSecondInsteadOfArrivalJitter() {
        assertEquals("123519", TrackSamplingPolicy.utcSecond("123519.80"))
        assertTrue(TrackSamplingPolicy.shouldStore("123518", "123519", 820))
        assertFalse(TrackSamplingPolicy.shouldStore("123519", "123519", 1_200))
        assertFalse(TrackSamplingPolicy.shouldStore(null, null, 999))
        assertTrue(TrackSamplingPolicy.shouldStore(null, null, 1_000))
    }

    @Test fun exposesOnlyDocumentedPointLimits() {
        assertEquals(listOf(50_000, 100_000, 300_000), TrackRetentionPolicy.ALLOWED_MAX_POINTS)
        TrackRetentionPolicy.ALLOWED_MAX_POINTS.forEach { assertTrue(TrackRetentionPolicy.isAllowed(it)) }
        assertFalse(TrackRetentionPolicy.isAllowed(49_999))
        assertFalse(TrackRetentionPolicy.isAllowed(300_001))
    }

    @Test fun defaultsInvalidOrMissingSettingsToFiftyThousand() {
        assertEquals(50_000, TrackRetentionPolicy.DEFAULT_MAX_POINTS)
        assertEquals(50_000, TrackRetentionPolicy.normalize(null))
        assertEquals(50_000, TrackRetentionPolicy.normalize(7))
        assertEquals(100_000, TrackRetentionPolicy.normalize(100_000))
        assertEquals(300_000, TrackRetentionPolicy.normalize(300_000))
    }

    @Test fun computesOnlyTheExcessThatMustBeDeleted() {
        assertEquals(0, TrackRetentionPolicy.excessPointCount(49_999, 50_000))
        assertEquals(0, TrackRetentionPolicy.excessPointCount(50_000, 50_000))
        assertEquals(1, TrackRetentionPolicy.excessPointCount(50_001, 50_000))
        assertEquals(250_000, TrackRetentionPolicy.excessPointCount(300_000, 50_000))
    }
}

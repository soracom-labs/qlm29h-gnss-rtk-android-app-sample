package jp.co.soracom.qlm29hrtk.soracom

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoracomSendPolicyTest {
    @Test fun defaultSendsValidFixButNotNoFix() {
        val policy = SoracomSendPolicy()
        assertFalse(policy.allows(0, true))
        assertTrue(policy.allows(1, true))
        assertTrue(policy.allows(2, false))
        assertTrue(policy.allows(6, true))
    }

    @Test fun canRequireNtrip() {
        val policy = SoracomSendPolicy(allowNtripDisconnected = false)
        assertFalse(policy.allows(4, false))
        assertTrue(policy.allows(4, true))
    }

    @Test fun filtersRtkQuality() {
        val floatOrBetter = SoracomSendPolicy(qualityPolicy = SoracomQualityPolicy.RTK_FLOAT_OR_BETTER)
        assertFalse(floatOrBetter.allows(2, true))
        assertTrue(floatOrBetter.allows(5, true))
        assertTrue(floatOrBetter.allows(4, true))
        val fixed = SoracomSendPolicy(qualityPolicy = SoracomQualityPolicy.RTK_FIXED_ONLY)
        assertFalse(fixed.allows(5, true))
        assertTrue(fixed.allows(4, true))
    }
}

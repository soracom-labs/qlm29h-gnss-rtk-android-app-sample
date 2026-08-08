package jp.co.soracom.qlm29hrtk.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartphoneGnssPolicyTest {
    @Test fun disabledOrUnpermittedCaptureNeverRuns() {
        assertFalse(SmartphoneGnssPolicy.decide(false, true, true, true).shouldCapture)
        assertFalse(SmartphoneGnssPolicy.decide(true, false, true, true).shouldCapture)
    }

    @Test fun foregroundCaptureDoesNotRequireAService() {
        val decision = SmartphoneGnssPolicy.decide(true, true, true, false)
        assertTrue(decision.shouldCapture)
        assertFalse(decision.requiresForegroundService)
    }

    @Test fun backgroundPreferenceKeepsCaptureAndRequiresAService() {
        val decision = SmartphoneGnssPolicy.decide(true, true, false, true)
        assertTrue(decision.shouldCapture)
        assertTrue(decision.requiresForegroundService)
    }

    @Test fun leavingForegroundPausesCaptureWhenBackgroundIsDisabled() {
        assertFalse(SmartphoneGnssPolicy.decide(true, true, false, false).shouldCapture)
    }
}

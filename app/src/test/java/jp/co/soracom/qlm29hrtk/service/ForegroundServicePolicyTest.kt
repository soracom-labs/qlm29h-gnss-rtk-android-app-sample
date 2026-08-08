package jp.co.soracom.qlm29hrtk.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundServicePolicyTest {
    @Test fun connectedDeviceRequiresAnActiveUsbPermission() {
        val detached = ForegroundServicePolicy.decide(true, false, false, false, false)
        assertFalse(detached.shouldRun)

        val connected = ForegroundServicePolicy.decide(true, true, false, false, false)
        assertTrue(connected.useConnectedDevice)
        assertTrue(connected.shouldRun)
    }

    @Test fun backgroundLocationRequiresEnabledCaptureAndPermission() {
        assertFalse(ForegroundServicePolicy.decide(false, false, true, true, false).useLocation)
        assertFalse(ForegroundServicePolicy.decide(false, false, true, false, true).useLocation)
        assertTrue(ForegroundServicePolicy.decide(false, false, true, true, true).useLocation)
    }

    @Test fun usbAndLocationCanRunTogether() {
        val result = ForegroundServicePolicy.decide(true, true, true, true, true)
        assertTrue(result.useConnectedDevice)
        assertTrue(result.useLocation)
    }
}

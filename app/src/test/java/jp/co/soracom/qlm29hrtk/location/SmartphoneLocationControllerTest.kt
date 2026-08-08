package jp.co.soracom.qlm29hrtk.location

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartphoneLocationControllerTest {
    @Test fun repeatedStartAndStopAreIdempotent() {
        val provider = RecordingProvider()
        val controller = SmartphoneLocationController(provider)

        controller.start { }.getOrThrow()
        controller.start { }.getOrThrow()
        assertTrue(controller.isRunning)
        assertEquals(1, provider.starts)

        controller.stop()
        controller.stop()
        assertFalse(controller.isRunning)
        assertEquals(1, provider.stops)
    }

    @Test fun missingProviderFailsWithoutEnteringRunningState() {
        val controller = SmartphoneLocationController(null)
        assertTrue(controller.start { }.isFailure)
        assertFalse(controller.isRunning)
    }

    private class RecordingProvider : SmartphoneLocationProvider {
        override val providerEnabled = true
        var starts = 0
        var stops = 0
        override fun start(listener: SmartphoneLocationListener) { starts++ }
        override fun stop() { stops++ }
    }
}

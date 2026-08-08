package jp.co.soracom.qlm29hrtk.soracom

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoracomScheduleControllerTest {
    @Test fun scheduleRequiresEnabledStateAndUsb() {
        assertNull(SoracomSchedulePolicy.intervalMillis(false, true, "5"))
        assertNull(SoracomSchedulePolicy.intervalMillis(true, false, "5"))
        assertEquals(5_000L, SoracomSchedulePolicy.intervalMillis(true, true, "invalid"))
        assertEquals(5_000L, SoracomSchedulePolicy.intervalMillis(true, true, "4"))
        assertEquals(3_600_000L, SoracomSchedulePolicy.intervalMillis(true, true, "3600"))
        assertEquals(3_600_000L, SoracomSchedulePolicy.intervalMillis(true, true, "3601"))
    }

    @Test fun clampsTheActualScheduleToAtLeastFiveSeconds() = runTest {
        var sends = 0
        val controller = SoracomScheduleController(this)
        controller.start(1_000) { sends++ }

        runCurrent()
        assertEquals(1, sends)
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, sends)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, sends)

        controller.stop()
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(2, sends)
    }
}

package jp.co.soracom.qlm29hrtk.soracom

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoracomScheduleControllerTest {
    @Test fun scheduleRequiresEnabledStateAndUsb() {
        assertNull(SoracomSchedulePolicy.intervalMillis(false, true, "60"))
        assertNull(SoracomSchedulePolicy.intervalMillis(true, false, "60"))
    }

    @Test fun acceptsOnlyDocumentedIntervalsAndFallsBackToSixtySeconds() {
        SoracomSchedulePolicy.ALLOWED_INTERVAL_SECONDS.forEach { seconds ->
            assertEquals(seconds * 1_000L, SoracomSchedulePolicy.intervalMillis(true, true, seconds.toString()))
        }
        listOf("invalid", "2", "4", "7", "61", "3600").forEach { interval ->
            assertEquals(60_000L, SoracomSchedulePolicy.intervalMillis(true, true, interval))
        }
    }

    @Test fun everyNonDefaultChoiceRequiresCostConfirmation() {
        assertEquals(listOf(30, 15, 10, 6, 5, 3), SoracomSchedulePolicy.HIGH_FREQUENCY_INTERVAL_SECONDS)
        SoracomSchedulePolicy.ALLOWED_INTERVAL_SECONDS.filterNot { it == 60 }.forEach { seconds ->
            assertTrue(SoracomSchedulePolicy.requiresCostConfirmation(seconds))
        }
        assertFalse(SoracomSchedulePolicy.requiresCostConfirmation(60))
        assertFalse(SoracomSchedulePolicy.requiresCostConfirmation(4))
    }

    @Test fun invalidActualScheduleFallsBackToSixtySeconds() = runTest {
        var sends = 0
        val controller = SoracomScheduleController(this)
        controller.start(1_000) { sends++ }

        runCurrent()
        assertEquals(1, sends)
        advanceTimeBy(59_999)
        runCurrent()
        assertEquals(1, sends)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, sends)

        controller.stop()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2, sends)
    }
}

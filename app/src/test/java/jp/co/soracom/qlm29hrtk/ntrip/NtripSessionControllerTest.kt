package jp.co.soracom.qlm29hrtk.ntrip

import java.io.IOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NtripSessionControllerTest {
    @Test fun failurePolicyStopsForAuthenticationAndTls() {
        assertTrue(NtripFailurePolicy.classify(IOException("HTTP 401")) is NtripSessionEvent.AuthError)
        assertTrue(NtripFailurePolicy.classify(NtripHttpException(403)) is NtripSessionEvent.AuthError)
        assertTrue(NtripFailurePolicy.classify(NtripHttpException(404)) is NtripSessionEvent.ConfigurationError)
        assertTrue(NtripFailurePolicy.classify(NtripHttpException(429)) is NtripSessionEvent.Reconnecting)
        assertTrue(NtripFailurePolicy.classify(NtripHttpException(503)) is NtripSessionEvent.Reconnecting)
        assertTrue(NtripFailurePolicy.classify(SSLException("certificate")) is NtripSessionEvent.TlsError)
        assertTrue(NtripFailurePolicy.classify(IOException("network lost")) is NtripSessionEvent.Reconnecting)
    }

    @Test fun retryPolicyUsesBoundedExponentialDelays() {
        val policy = NtripRetryPolicy(
            initialDelayMillis = 1_000,
            maximumDelayMillis = 60_000,
            jitterRatio = 0.0,
        )

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L),
            (1..8).map(policy::delayMillis))
    }

    @Test fun retryPolicyAppliesConfiguredJitterBounds() {
        val upper = NtripRetryPolicy(1_000, 60_000, 0.20) { span -> span }
        val lower = NtripRetryPolicy(1_000, 60_000, 0.20) { span -> -span }

        assertEquals(1_200, upper.delayMillis(1))
        assertEquals(800, lower.delayMillis(1))
        assertEquals(60_000, upper.delayMillis(8))
        assertEquals(48_000, lower.delayMillis(8))
    }

    @Test fun transientFailureRetriesButAuthenticationFailureStops() = runTest {
        val source = SequencedSource(listOf(IOException("network lost"), IOException("Unauthorized")))
        val events = mutableListOf<NtripSessionEvent>()
        val controller = NtripSessionController(
            source,
            this,
            retryPolicy = NtripRetryPolicy(100, 100, jitterRatio = 0.0),
        )

        controller.connect(config(), { "GGA" }, events::add) { }
        runCurrent()
        assertEquals(1, source.attempts)
        assertTrue(events.last() is NtripSessionEvent.Reconnecting)

        advanceTimeBy(100)
        runCurrent()
        assertEquals(2, source.attempts)
        assertTrue(events.last() is NtripSessionEvent.AuthError)
    }

    @Test fun disconnectCancelsTheOwnedStreamingJob() = runTest {
        val source = SequencedSource(listOf(IOException("network lost")))
        val events = mutableListOf<NtripSessionEvent>()
        val controller = NtripSessionController(
            source,
            this,
            retryPolicy = NtripRetryPolicy(10_000, 10_000, jitterRatio = 0.0),
        )
        controller.connect(config(), { "GGA" }, events::add) { }
        runCurrent()

        controller.disconnect()
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()

        // The owner sets its UI state to Disconnected synchronously. The
        // controller contract is that cancellation prevents another attempt.
        assertEquals(1, source.attempts)
    }

    @Test fun networkRecoveryWakesAPendingBackoffWithoutStartingASecondJob() = runTest {
        val source = SequencedSource(listOf(IOException("offline"), IOException("still unavailable")))
        val controller = NtripSessionController(
            source,
            this,
            retryPolicy = NtripRetryPolicy(10_000, 10_000, jitterRatio = 0.0),
        )

        controller.connect(config(), { "GGA" }, { }) { }
        runCurrent()
        assertEquals(1, source.attempts)

        controller.onNetworkAvailable()
        runCurrent()

        assertEquals(2, source.attempts)
        controller.disconnect()
    }

    @Test fun stableRtcmReceptionResetsTheConsecutiveFailureBackoff() = runTest {
        var attempts = 0
        val source = object : NtripDataSource {
            override suspend fun fetchSourceTable(config: NtripConfig): List<MountPoint> = emptyList()
            override suspend fun stream(
                config: NtripConfig,
                latestGga: () -> String?,
                onConnected: suspend () -> Unit,
                onRtcm: suspend (ByteArray) -> Unit,
            ): Nothing {
                attempts++
                if (attempts == 1) throw IOException("first outage")
                onConnected()
                onRtcm(byteArrayOf(1))
                delay(30)
                onRtcm(byteArrayOf(2))
                throw IOException("outage after stable reception")
            }
        }
        val events = mutableListOf<NtripSessionEvent>()
        val controller = NtripSessionController(
            source,
            this,
            retryPolicy = NtripRetryPolicy(100, 100, jitterRatio = 0.0),
            monotonicMillis = { testScheduler.currentTime },
            stableReceiveMillis = 30,
        )

        controller.connect(config(), { "GGA" }, events::add) { }
        runCurrent()
        advanceTimeBy(100)
        runCurrent()
        advanceTimeBy(30)
        runCurrent()

        val reconnects = events.filterIsInstance<NtripSessionEvent.Reconnecting>()
        assertEquals(listOf(1, 1), reconnects.map { it.attempt })
        assertTrue(events.any { it is NtripSessionEvent.Stable })
        controller.disconnect()
    }

    @Test fun waitsForAValidGgaWithoutOpeningAConnection() = runTest {
        val source = SequencedSource(listOf(IOException("should not connect")))
        val events = mutableListOf<NtripSessionEvent>()
        val controller = NtripSessionController(source, this)

        controller.connect(config(), { null }, events::add) { }
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(0, source.attempts)
        assertTrue(events.last() is NtripSessionEvent.WaitingForGga)
        controller.disconnect()
    }

    private fun config() = NtripConfig("example.invalid", 2101, "AUTO", "", "", false)

    private class SequencedSource(private val failures: List<Throwable>) : NtripDataSource {
        var attempts = 0
        override suspend fun fetchSourceTable(config: NtripConfig): List<MountPoint> = emptyList()
        override suspend fun stream(
            config: NtripConfig,
            latestGga: () -> String?,
            onConnected: suspend () -> Unit,
            onRtcm: suspend (ByteArray) -> Unit,
        ): Nothing {
            val failure = failures.getOrElse(attempts) { failures.last() }
            attempts++
            throw failure
        }
    }
}

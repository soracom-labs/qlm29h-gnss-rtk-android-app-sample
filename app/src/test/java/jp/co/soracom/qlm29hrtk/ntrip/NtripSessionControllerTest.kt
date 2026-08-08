package jp.co.soracom.qlm29hrtk.ntrip

import java.io.IOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        assertTrue(NtripFailurePolicy.classify(SSLException("certificate")) is NtripSessionEvent.TlsError)
        assertTrue(NtripFailurePolicy.classify(IOException("network lost")) is NtripSessionEvent.Reconnecting)
    }

    @Test fun transientFailureRetriesButAuthenticationFailureStops() = runTest {
        val source = SequencedSource(listOf(IOException("network lost"), IOException("Unauthorized")))
        val events = mutableListOf<NtripSessionEvent>()
        val controller = NtripSessionController(source, this, retryDelayMillis = 100)

        controller.connect(config(), { null }, events::add) { }
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
        val controller = NtripSessionController(source, this, retryDelayMillis = 10_000)
        controller.connect(config(), { null }, events::add) { }
        runCurrent()

        controller.disconnect()
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()

        // The owner sets its UI state to Disconnected synchronously. The
        // controller contract is that cancellation prevents another attempt.
        assertEquals(1, source.attempts)
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

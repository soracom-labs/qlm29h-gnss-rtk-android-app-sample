package jp.co.soracom.qlm29hrtk.ntrip

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong
import kotlin.random.Random
import javax.net.ssl.SSLException

sealed interface NtripSessionEvent {
    data object Connecting : NtripSessionEvent
    data object WaitingForGga : NtripSessionEvent
    data object Connected : NtripSessionEvent
    data object Stable : NtripSessionEvent
    data object Disconnected : NtripSessionEvent
    data class Reconnecting(
        val message: String,
        val attempt: Int,
        val delayMillis: Long,
    ) : NtripSessionEvent
    data class AuthError(val message: String = "NTRIP authentication failed") : NtripSessionEvent
    data class TlsError(val message: String) : NtripSessionEvent
    data class ConfigurationError(val message: String) : NtripSessionEvent
}

object NtripFailurePolicy {
    fun classify(error: Throwable): NtripSessionEvent {
        val message = error.message ?: "NTRIP failed"
        return when {
            error is NtripHttpException && error.statusCode in setOf(401, 403) -> NtripSessionEvent.AuthError()
            message.contains("401") || message.contains("Unauthorized", ignoreCase = true) -> NtripSessionEvent.AuthError()
            error is NtripHttpException && error.statusCode in 400..499 && error.statusCode !in setOf(408, 429) ->
                NtripSessionEvent.ConfigurationError("Check the NTRIP host and mount point")
            error is SSLException -> NtripSessionEvent.TlsError(message)
            else -> NtripSessionEvent.Reconnecting(message, attempt = 0, delayMillis = 0)
        }
    }
}

/** NTRIP-07: bounded exponential retry avoids reconnect storms during field outages. */
class NtripRetryPolicy(
    private val initialDelayMillis: Long = 1_000,
    private val maximumDelayMillis: Long = 60_000,
    private val jitterRatio: Double = 0.20,
    private val jitterOffset: (Long) -> Long = { span ->
        if (span == 0L) 0L else Random.nextLong(-span, span + 1)
    },
) {
    init {
        require(initialDelayMillis > 0)
        require(maximumDelayMillis >= initialDelayMillis)
        require(jitterRatio in 0.0..1.0)
    }

    fun delayMillis(attempt: Int): Long {
        require(attempt > 0)
        var base = initialDelayMillis
        var remainingDoublings = attempt - 1
        while (remainingDoublings > 0 && base < maximumDelayMillis) {
            base = if (base > maximumDelayMillis / 2) maximumDelayMillis else base * 2
            remainingDoublings--
        }
        val jitterSpan = (base * jitterRatio).roundToLong()
        return (base + jitterOffset(jitterSpan)).coerceIn(0, maximumDelayMillis)
    }
}

/** Owns the single NTRIP streaming job and its retry lifecycle. */
class NtripSessionController(
    private val client: NtripDataSource,
    private val scope: CoroutineScope,
    private val retryPolicy: NtripRetryPolicy = NtripRetryPolicy(),
    private val monotonicMillis: () -> Long = { System.nanoTime() / 1_000_000 },
    private val stableReceiveMillis: Long = 30_000,
) {
    private var job: Job? = null
    private var generation: Long = 0
    private val networkAvailable = Channel<Unit>(Channel.CONFLATED)

    fun connect(
        config: NtripConfig,
        latestGga: () -> String?,
        onEvent: (NtripSessionEvent) -> Unit,
        onRtcm: suspend (ByteArray) -> Unit,
    ) {
        val activeGeneration = ++generation
        fun emit(event: NtripSessionEvent) {
            if (generation == activeGeneration) onEvent(event)
        }
        job?.cancel()
        while (networkAvailable.tryReceive().isSuccess) Unit
        emit(NtripSessionEvent.Connecting)
        job = scope.launch {
            var consecutiveFailures = 0
            while (true) {
                try {
                    var waitingForGgaReported = false
                    while (latestGga() == null) {
                        if (!waitingForGgaReported) {
                            emit(NtripSessionEvent.WaitingForGga)
                            waitingForGgaReported = true
                        }
                        delay(1_000)
                    }
                    emit(NtripSessionEvent.Connecting)
                    var receivingSince: Long? = null
                    var stableReported = false
                    client.stream(
                        config = config,
                        latestGga = latestGga,
                        onConnected = { emit(NtripSessionEvent.Connected) },
                        onRtcm = { bytes ->
                            onRtcm(bytes)
                            val now = monotonicMillis()
                            val since = receivingSince ?: now.also { receivingSince = it }
                            if (!stableReported && now - since >= stableReceiveMillis) {
                                consecutiveFailures = 0
                                stableReported = true
                                while (networkAvailable.tryReceive().isSuccess) Unit
                                emit(NtripSessionEvent.Stable)
                            }
                        },
                    )
                } catch (_: CancellationException) {
                    emit(NtripSessionEvent.Disconnected)
                    break
                } catch (error: Throwable) {
                    when (val event = NtripFailurePolicy.classify(error)) {
                        is NtripSessionEvent.AuthError,
                        is NtripSessionEvent.TlsError,
                        is NtripSessionEvent.ConfigurationError,
                        -> {
                            emit(event)
                            break
                        }
                        is NtripSessionEvent.Reconnecting -> {
                            consecutiveFailures++
                            val delayMillis = retryPolicy.delayMillis(consecutiveFailures)
                            emit(event.copy(attempt = consecutiveFailures, delayMillis = delayMillis))
                            // A ConnectivityManager onAvailable event wakes a
                            // pending retry immediately without creating a
                            // second stream Job or resetting the backoff state.
                            withTimeoutOrNull(delayMillis) { networkAvailable.receive() }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun disconnect() {
        generation++
        job?.cancel()
        job = null
    }

    fun onNetworkAvailable() {
        if (job?.isActive == true) networkAvailable.trySend(Unit)
    }
}

package jp.co.soracom.qlm29hrtk.ntrip

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.net.ssl.SSLException

sealed interface NtripSessionEvent {
    data object Connecting : NtripSessionEvent
    data object Connected : NtripSessionEvent
    data object Disconnected : NtripSessionEvent
    data class Reconnecting(val message: String) : NtripSessionEvent
    data class AuthError(val message: String = "NTRIP authentication failed") : NtripSessionEvent
    data class TlsError(val message: String) : NtripSessionEvent
}

object NtripFailurePolicy {
    fun classify(error: Throwable): NtripSessionEvent {
        val message = error.message ?: "NTRIP failed"
        return when {
            message.contains("401") || message.contains("Unauthorized", ignoreCase = true) -> NtripSessionEvent.AuthError()
            error is SSLException -> NtripSessionEvent.TlsError(message)
            else -> NtripSessionEvent.Reconnecting(message)
        }
    }
}

/** Owns the single NTRIP streaming job and its retry lifecycle. */
class NtripSessionController(
    private val client: NtripDataSource,
    private val scope: CoroutineScope,
    private val retryDelayMillis: Long = 5_000,
) {
    private var job: Job? = null

    fun connect(
        config: NtripConfig,
        latestGga: () -> String?,
        onEvent: (NtripSessionEvent) -> Unit,
        onRtcm: suspend (ByteArray) -> Unit,
    ) {
        job?.cancel()
        onEvent(NtripSessionEvent.Connecting)
        job = scope.launch {
            while (true) {
                try {
                    client.stream(
                        config = config,
                        latestGga = latestGga,
                        onConnected = { onEvent(NtripSessionEvent.Connected) },
                        onRtcm = onRtcm,
                    )
                } catch (_: CancellationException) {
                    onEvent(NtripSessionEvent.Disconnected)
                    break
                } catch (error: Throwable) {
                    when (val event = NtripFailurePolicy.classify(error)) {
                        is NtripSessionEvent.AuthError, is NtripSessionEvent.TlsError -> {
                            onEvent(event)
                            break
                        }
                        is NtripSessionEvent.Reconnecting -> {
                            onEvent(event)
                            delay(retryDelayMillis)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    fun disconnect() {
        job?.cancel()
        job = null
    }
}

package jp.co.soracom.qlm29hrtk.soracom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoracomSchedulePolicy {
    const val MIN_INTERVAL_SECONDS = 5
    const val MAX_INTERVAL_SECONDS = 3_600
    const val DEFAULT_INTERVAL_SECONDS = 5

    fun intervalMillis(enabled: Boolean, usbConnected: Boolean, seconds: String): Long? {
        if (!enabled || !usbConnected) return null
        val interval = seconds.toLongOrNull()?.coerceIn(
            MIN_INTERVAL_SECONDS.toLong(),
            MAX_INTERVAL_SECONDS.toLong(),
        ) ?: DEFAULT_INTERVAL_SECONDS.toLong()
        return interval * 1_000
    }
}

/** Owns only periodic timing; payload policy and network I/O remain separate. */
class SoracomScheduleController(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun start(intervalMillis: Long, send: suspend () -> Unit) {
        job?.cancel()
        // SORACOM-05: transient UI or caller errors must never schedule faster than five seconds.
        val safeIntervalMillis = intervalMillis.coerceIn(
            SoracomSchedulePolicy.MIN_INTERVAL_SECONDS * 1_000L,
            SoracomSchedulePolicy.MAX_INTERVAL_SECONDS * 1_000L,
        )
        job = scope.launch {
            while (true) {
                send()
                delay(safeIntervalMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

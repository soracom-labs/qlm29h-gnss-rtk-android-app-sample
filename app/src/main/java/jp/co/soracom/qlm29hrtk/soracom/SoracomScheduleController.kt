package jp.co.soracom.qlm29hrtk.soracom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoracomSchedulePolicy {
    val ALLOWED_INTERVAL_SECONDS = listOf(3, 5, 6, 10, 15, 30, 60)
    const val DEFAULT_INTERVAL_SECONDS = 60
    const val DEFAULT_INTERVAL_MILLIS = DEFAULT_INTERVAL_SECONDS * 1_000L

    fun isAllowedInterval(seconds: Int): Boolean = seconds in ALLOWED_INTERVAL_SECONDS

    fun requiresCostConfirmation(seconds: Int): Boolean =
        isAllowedInterval(seconds) && seconds != DEFAULT_INTERVAL_SECONDS

    fun intervalMillis(enabled: Boolean, usbConnected: Boolean, seconds: String): Long? {
        if (!enabled || !usbConnected) return null
        val interval = seconds.toIntOrNull()?.takeIf(::isAllowedInterval) ?: DEFAULT_INTERVAL_SECONDS
        return interval * 1_000L
    }
}

/** Owns only periodic timing; payload policy and network I/O remain separate. */
class SoracomScheduleController(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun start(intervalMillis: Long, send: suspend () -> Unit) {
        job?.cancel()
        // SORACOM-05: bypassing the UI must not create an unconfirmed high-frequency schedule.
        val safeIntervalMillis = intervalMillis.takeIf { candidate ->
            SoracomSchedulePolicy.ALLOWED_INTERVAL_SECONDS.any { it * 1_000L == candidate }
        } ?: SoracomSchedulePolicy.DEFAULT_INTERVAL_MILLIS
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

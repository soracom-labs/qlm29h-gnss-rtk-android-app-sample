package jp.co.soracom.qlm29hrtk.soracom

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object SoracomSchedulePolicy {
    fun intervalMillis(enabled: Boolean, usbConnected: Boolean, seconds: String): Long? {
        if (!enabled || !usbConnected) return null
        val interval = seconds.toLongOrNull()?.takeIf { it in 1..3_600 } ?: 5L
        return interval * 1_000
    }
}

/** Owns only periodic timing; payload policy and network I/O remain separate. */
class SoracomScheduleController(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun start(intervalMillis: Long, send: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            while (true) {
                send()
                delay(intervalMillis)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

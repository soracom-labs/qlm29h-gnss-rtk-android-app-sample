package jp.co.soracom.qlm29hrtk.location

/** Owns LocationProvider registration and makes start/stop idempotent. */
class SmartphoneLocationController(private val provider: SmartphoneLocationProvider?) {
    var isRunning: Boolean = false
        private set

    val providerEnabled: Boolean get() = provider?.providerEnabled == true

    fun start(listener: SmartphoneLocationListener): Result<Unit> {
        if (isRunning) return Result.success(Unit)
        return runCatching {
            provider?.start(listener) ?: error("Smartphone location provider is unavailable")
            isRunning = true
        }
    }

    fun stop() {
        if (!isRunning) return
        provider?.stop()
        isRunning = false
    }
}

package jp.co.soracom.qlm29hrtk.location

data class SmartphoneGnssDecision(
    val shouldCapture: Boolean,
    val requiresForegroundService: Boolean,
)

/**
 * SP-03/SP-05 lifecycle rule, independent from Android LocationManager.
 * Background capture is allowed only after explicit runtime enablement and a
 * currently valid precise-location permission.
 */
object SmartphoneGnssPolicy {
    fun decide(
        enabled: Boolean,
        permissionGranted: Boolean,
        appInForeground: Boolean,
        backgroundEnabled: Boolean,
    ): SmartphoneGnssDecision {
        val capture = enabled && permissionGranted && (appInForeground || backgroundEnabled)
        return SmartphoneGnssDecision(
            shouldCapture = capture,
            requiresForegroundService = capture && backgroundEnabled,
        )
    }
}

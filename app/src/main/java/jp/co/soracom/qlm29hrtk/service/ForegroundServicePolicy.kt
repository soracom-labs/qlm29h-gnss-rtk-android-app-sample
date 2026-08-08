package jp.co.soracom.qlm29hrtk.service

/**
 * Platform-independent decision for foreground-service capabilities.
 *
 * Android 14+ validates a service type against the permission valid at the
 * exact time startForeground is called. Keeping this decision pure makes USB
 * detach and location-permission regressions testable without a device.
 * See requirements FGS-01 through FGS-03.
 */
data class ForegroundServiceDecision(
    val useConnectedDevice: Boolean,
    val useLocation: Boolean,
) {
    val shouldRun: Boolean get() = useConnectedDevice || useLocation
}

object ForegroundServicePolicy {
    fun decide(
        usbActive: Boolean,
        hasUsbPermission: Boolean,
        smartphoneGnssEnabled: Boolean,
        smartphoneGnssBackground: Boolean,
        hasLocationPermission: Boolean,
    ): ForegroundServiceDecision = ForegroundServiceDecision(
        useConnectedDevice = usbActive && hasUsbPermission,
        useLocation = smartphoneGnssEnabled && smartphoneGnssBackground && hasLocationPermission,
    )
}

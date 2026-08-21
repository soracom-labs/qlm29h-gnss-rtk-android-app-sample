package jp.co.soracom.qlm29hrtk.network

enum class InternetReachability(val label: String) {
    OFFLINE("Offline"),
    CHECKING("Checking"),
    ONLINE("Online"),
}

/** NET-01: a transport alone is not proof that the public Internet is usable. */
object InternetReachabilityPolicy {
    fun evaluate(
        hasNetwork: Boolean,
        hasInternetCapability: Boolean,
        isValidated: Boolean,
    ): InternetReachability = when {
        !hasNetwork || !hasInternetCapability -> InternetReachability.OFFLINE
        isValidated -> InternetReachability.ONLINE
        else -> InternetReachability.CHECKING
    }
}

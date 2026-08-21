package jp.co.soracom.qlm29hrtk.network

enum class InternetReachability(val label: String) {
    OFFLINE("Offline"),
    CHECKING("Checking"),
    ONLINE("Online"),
}

data class InternetNetworkCandidate(
    val isVpn: Boolean,
    val hasInternetCapability: Boolean,
    val isValidated: Boolean,
)

/** NET-01: a retained VPN must not hide loss of every validated underlay. */
object InternetReachabilityPolicy {
    fun evaluate(candidates: Collection<InternetNetworkCandidate>): InternetReachability {
        val physicalCandidates = candidates.filterNot(InternetNetworkCandidate::isVpn)
        val internetCandidates = physicalCandidates.filter(InternetNetworkCandidate::hasInternetCapability)
        return when {
            internetCandidates.isEmpty() -> InternetReachability.OFFLINE
            internetCandidates.any(InternetNetworkCandidate::isValidated) -> InternetReachability.ONLINE
            else -> InternetReachability.CHECKING
        }
    }
}

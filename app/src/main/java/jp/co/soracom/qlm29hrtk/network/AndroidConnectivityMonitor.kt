package jp.co.soracom.qlm29hrtk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper

data class AndroidNetworkStatus(
    val internet: InternetReachability,
)

/** Application-scoped owner of the single Internet-capable network callback. */
class AndroidConnectivityMonitor(
    context: Context,
    private val onStatusChanged: (AndroidNetworkStatus) -> Unit,
) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val callbackHandler = Handler(Looper.getMainLooper())
    private val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    private val candidates = mutableMapOf<Network, InternetNetworkCandidate>()
    private var started = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            manager.getNetworkCapabilities(network)?.let { update(network, it) }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            update(network, capabilities)
        }

        override fun onLost(network: Network) {
            candidates.remove(network)
            reportCurrent()
        }
    }

    fun start() {
        if (started) return
        started = true
        // NET-01: observe every Internet candidate. A per-app VPN can remain
        // VALIDATED after all of its Wi-Fi/Cellular underlays have disappeared.
        manager.registerNetworkCallback(request, callback, callbackHandler)
        reportCurrent()
    }

    private fun update(network: Network, capabilities: NetworkCapabilities) {
        candidates[network] = InternetNetworkCandidate(
            isVpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            hasInternetCapability = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
        )
        reportCurrent()
    }

    private fun reportCurrent() {
        onStatusChanged(
            AndroidNetworkStatus(
                internet = InternetReachabilityPolicy.evaluate(candidates.values),
            ),
        )
    }
}

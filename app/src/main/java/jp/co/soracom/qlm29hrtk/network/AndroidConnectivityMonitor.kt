package jp.co.soracom.qlm29hrtk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

data class AndroidNetworkStatus(
    val hasNetwork: Boolean,
    val hasInternetCapability: Boolean,
    val isValidated: Boolean,
)

/** Application-scoped owner of the single default-network callback. */
class AndroidConnectivityMonitor(
    context: Context,
    private val onStatusChanged: (AndroidNetworkStatus) -> Unit,
) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private var started = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = reportCurrent()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = reportCurrent()
        override fun onLost(network: Network) = reportCurrent()
    }

    fun start() {
        if (started) return
        started = true
        manager.registerDefaultNetworkCallback(callback)
        reportCurrent()
    }

    private fun reportCurrent() {
        val activeNetwork = manager.activeNetwork
        val capabilities = activeNetwork?.let(manager::getNetworkCapabilities)
        onStatusChanged(
            AndroidNetworkStatus(
                hasNetwork = activeNetwork != null,
                hasInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
            ),
        )
    }
}

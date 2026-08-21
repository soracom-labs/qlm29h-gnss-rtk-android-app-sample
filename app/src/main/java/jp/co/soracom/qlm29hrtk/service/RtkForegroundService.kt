package jp.co.soracom.qlm29hrtk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import jp.co.soracom.qlm29hrtk.MainActivity
import jp.co.soracom.qlm29hrtk.R
import jp.co.soracom.qlm29hrtk.RtkApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RtkForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val runtime get() = (application as RtkApplication).runtime
    private val notificationManager get() = getSystemService(NotificationManager::class.java)
    private val connectivityManager get() = getSystemService(ConnectivityManager::class.java)
    private val usbManager get() = getSystemService(UsbManager::class.java)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = runtime.onNetworkAvailable()
        override fun onLost(network: Network) = runtime.onNetworkLost()
    }
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> runtime.handleUsbAttached()
                UsbManager.ACTION_USB_DEVICE_DETACHED -> runtime.onDeviceDetached()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "RTK connection", NotificationManager.IMPORTANCE_LOW),
        )
        registerReceiver(usbReceiver, IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, Context.RECEIVER_NOT_EXPORTED)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        updateForeground(runtime.state.value)
        scope.launch {
            runtime.state.collectLatest(::updateForeground)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            runtime.updateSmartphoneGnssEnabled(false)
            runtime.disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(usbReceiver) }
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateForeground(state: jp.co.soracom.qlm29hrtk.AppState) {
        val hasUsbPermission = usbManager.deviceList.values.any(usbManager::hasPermission)
        val decision = ForegroundServicePolicy.decide(
            usbActive = state.usb.connection.isActive,
            hasUsbPermission = hasUsbPermission,
            smartphoneGnssEnabled = state.smartphone.enabled,
            smartphoneGnssBackground = state.smartphone.background,
            hasLocationPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED,
        )
        // Android 14+ rejects a connectedDevice FGS as soon as USB permission is
        // lost (for example, while reacting to detach or disabling phone GNSS).
        if (!decision.shouldRun) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        var types = 0
        if (decision.useConnectedDevice) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        if (decision.useLocation) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(state), types)
        }.onFailure {
            if (state.smartphone.enabled) runtime.updateSmartphoneGnssEnabled(false)
            runtime.showError("Background operation stopped because its permission is no longer available")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(state: jp.co.soracom.qlm29hrtk.AppState): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, RtkForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("QLM29H RTK")
            .setContentText("USB: ${state.usb.connection.label} · NTRIP: ${state.ntrip.connection.label} · SP: ${state.smartphone.status.label} · Fix: ${state.tracking.latestFix?.qualityLabel ?: "No data"}")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopIntent)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "rtk_connection"
        const val NOTIFICATION_ID = 2901
        const val ACTION_STOP = "jp.co.soracom.qlm29hrtk.STOP"
    }
}

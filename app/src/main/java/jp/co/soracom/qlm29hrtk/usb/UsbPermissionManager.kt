package jp.co.soracom.qlm29hrtk.usb

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

class UsbPermissionManager(context: Context) {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    fun hasPermission(deviceId: Int): Boolean =
        usbManager.deviceList.values.firstOrNull { it.deviceId == deviceId }?.let(usbManager::hasPermission) == true

    fun request(deviceId: Int): Boolean {
        val device = usbManager.deviceList.values.firstOrNull { it.deviceId == deviceId } ?: return false
        val intent = PendingIntent.getBroadcast(
            appContext,
            deviceId,
            Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        usbManager.requestPermission(device, intent)
        return true
    }

    companion object {
        const val ACTION_USB_PERMISSION = "jp.co.soracom.qlm29hrtk.USB_PERMISSION"
    }
}

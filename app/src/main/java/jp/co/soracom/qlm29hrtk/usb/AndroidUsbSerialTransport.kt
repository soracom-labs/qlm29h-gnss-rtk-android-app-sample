package jp.co.soracom.qlm29hrtk.usb

import android.content.Context
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidUsbSerialTransport(context: Context) : SerialTransport, SerialInputOutputManager.Listener {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
    private val data = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incoming = data.asSharedFlow()
    private val mutex = Mutex()
    private var port: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    override suspend fun listDevices(): List<UsbSerialDevice> = UsbSerialProber.getDefaultProber()
        .findAllDrivers(usbManager).map { driver ->
            val device = driver.device
            UsbSerialDevice(device.deviceId, device.deviceName, device.vendorId, device.productId, driver.javaClass.simpleName)
        }

    override suspend fun hasPermission(deviceId: Int): Boolean = usbManager.deviceList.values
        .firstOrNull { it.deviceId == deviceId }
        ?.let(usbManager::hasPermission) == true

    override suspend fun connect(deviceId: Int) = mutex.withLock {
        closeLocked()
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            .firstOrNull { it.device.deviceId == deviceId } ?: error("USB serial device not found")
        if (!usbManager.hasPermission(driver.device)) error("USB permission is required")
        val connection = usbManager.openDevice(driver.device) ?: error("Unable to open USB device")
        val opened = driver.ports.firstOrNull() ?: error("USB serial port not found")
        try {
            opened.open(connection)
            opened.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port = opened
            ioManager = SerialInputOutputManager(opened, this).also { it.start() }
        } catch (error: Throwable) {
            connection.close()
            throw error
        }
    }

    override suspend fun write(bytes: ByteArray) = mutex.withLock {
        (port ?: error("USB serial port is not connected")).write(bytes, 1_000)
    }

    override suspend fun disconnect() = mutex.withLock { closeLocked() }
    override fun onNewData(bytes: ByteArray) { data.tryEmit(bytes.copyOf()) }
    override fun onRunError(error: Exception) { ioManager = null }

    private fun closeLocked() {
        ioManager?.stop()
        ioManager = null
        runCatching { port?.close() }
        port = null
    }
}

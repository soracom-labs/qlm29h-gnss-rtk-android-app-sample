package jp.co.soracom.qlm29hrtk.usb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Sole owner of the serial receive Job. Starting a new connection replaces the
 * previous reader so USB-03 cannot leave two consumers parsing the same stream.
 */
class UsbSessionController(
    private val transport: SerialTransport,
    private val scope: CoroutineScope,
) {
    private var readerJob: Job? = null

    suspend fun listDevices(): List<UsbSerialDevice> = transport.listDevices()
    suspend fun hasPermission(deviceId: Int): Boolean = transport.hasPermission(deviceId)

    suspend fun connect(
        deviceId: Int,
        beforeRead: suspend () -> Unit = {},
        onBytes: (ByteArray) -> Unit,
    ) {
        transport.connect(deviceId)
        // DATA-01/DATA-06: establish persistence before collecting so the
        // first receiver bytes cannot precede their session boundary.
        beforeRead()
        readerJob?.cancel()
        readerJob = scope.launch { transport.incoming.collect(onBytes) }
    }

    suspend fun write(bytes: ByteArray) = transport.write(bytes)

    suspend fun disconnect() {
        stopReading()
        transport.disconnect()
    }

    fun stopReading() {
        readerJob?.cancel()
        readerJob = null
    }
}

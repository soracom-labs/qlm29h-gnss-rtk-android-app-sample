package jp.co.soracom.qlm29hrtk.usb

import kotlinx.coroutines.flow.Flow

data class UsbSerialDevice(
    val id: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val driver: String,
)

interface SerialTransport {
    val incoming: Flow<ByteArray>
    suspend fun listDevices(): List<UsbSerialDevice>
    suspend fun hasPermission(deviceId: Int): Boolean
    suspend fun connect(deviceId: Int)
    suspend fun write(bytes: ByteArray)
    suspend fun disconnect()
}

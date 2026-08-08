package jp.co.soracom.qlm29hrtk.usb

import kotlinx.coroutines.flow.MutableSharedFlow

class FakeSerialTransport : SerialTransport {
    override val incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 32)
    val writes = mutableListOf<ByteArray>()
    var connectCount = 0
    var disconnectCount = 0
    override suspend fun listDevices() = listOf(UsbSerialDevice(1, "Fake QLM29H", 0x2c7c, 0x0000, "Fake"))
    override suspend fun hasPermission(deviceId: Int) = true
    override suspend fun connect(deviceId: Int) { connectCount++ }
    override suspend fun write(bytes: ByteArray) { writes += bytes.copyOf() }
    override suspend fun disconnect() { disconnectCount++ }
    suspend fun emit(sentence: String) { incoming.emit(sentence.toByteArray(Charsets.US_ASCII)) }
}

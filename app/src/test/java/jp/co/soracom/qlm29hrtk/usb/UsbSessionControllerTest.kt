package jp.co.soracom.qlm29hrtk.usb

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UsbSessionControllerTest {
    @Test fun connectionOwnsReaderAndDelegatesWrites() = runTest {
        val transport = FakeSerialTransport()
        val received = mutableListOf<String>()
        val controller = UsbSessionController(transport, this)
        var persistenceReady = false

        controller.connect(
            deviceId = 1,
            beforeRead = {
                assertEquals(1, transport.connectCount)
                persistenceReady = true
            },
            onBytes = { received += it.toString(Charsets.US_ASCII) },
        )
        runCurrent()
        transport.emit("first")
        runCurrent()
        controller.write(byteArrayOf(1, 2, 3))

        assertEquals(1, transport.connectCount)
        assertTrue(persistenceReady)
        assertEquals(listOf("first"), received)
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(transport.writes.single()))
        controller.stopReading()
    }

    @Test fun reconnectReplacesReaderAndDisconnectStopsIt() = runTest {
        val transport = FakeSerialTransport()
        var received = 0
        val controller = UsbSessionController(transport, this)

        controller.connect(1) { received++ }
        runCurrent()
        controller.connect(1) { received++ }
        runCurrent()
        transport.emit("one")
        runCurrent()
        assertEquals(1, received)

        controller.disconnect()
        transport.emit("ignored")
        runCurrent()
        assertEquals(1, received)
        assertEquals(1, transport.disconnectCount)
    }
}

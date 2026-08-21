package jp.co.soracom.qlm29hrtk

import jp.co.soracom.qlm29hrtk.service.ForegroundController
import jp.co.soracom.qlm29hrtk.network.InternetReachability
import jp.co.soracom.qlm29hrtk.soracom.NetworkTypeProvider
import jp.co.soracom.qlm29hrtk.usb.FakeSerialTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RtkRuntimeIntegrationTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun autoConnectsFakeUsbAndSendsRtkInitialization() {
        val serial = FakeSerialTransport()
        val foreground = RecordingForegroundController()
        val runtime = RtkRuntime(serial, foregroundController = foreground)

        runtime.handleUsbAttached()

        assertEquals(UsbConnectionState.CONNECTED, runtime.state.value.usb.connection)
        assertEquals(1, serial.writes.size)
        assertEquals("\$PQTMCFGRTK,W,1,1*6C\r\n", serial.writes.single().toString(Charsets.US_ASCII))
        assertTrue(foreground.started)

        runtime.disconnect()
        assertEquals(UsbConnectionState.DISCONNECTED, runtime.state.value.usb.connection)
        assertTrue(foreground.stopped)
    }

    @Test fun soracomCannotBeEnabledWithoutConnectedUsb() {
        val runtime = RtkRuntime(FakeSerialTransport())

        runtime.updateSoracomEnabled(true)

        assertEquals(false, runtime.state.value.soracom.enabled)
        assertEquals(SoracomPublicationState.DISABLED, runtime.state.value.soracom.status)
        assertEquals("Connect USB before enabling SORACOM", runtime.state.value.notice.error)
    }

    @Test fun internetTurnsOnlineOnlyAfterAndroidValidation() {
        val runtime = RtkRuntime(
            FakeSerialTransport(),
            networkTypeProvider = NetworkTypeProvider { "Wi-Fi" },
        )

        runtime.onNetworkStatusChanged(hasNetwork = true, hasInternetCapability = true, isValidated = false)
        assertEquals(InternetReachability.CHECKING, runtime.state.value.connectivity.internet)
        assertEquals("Wi-Fi", runtime.state.value.soracom.networkType)

        runtime.onNetworkStatusChanged(hasNetwork = true, hasInternetCapability = true, isValidated = true)
        assertEquals(InternetReachability.ONLINE, runtime.state.value.connectivity.internet)

        runtime.onNetworkStatusChanged(hasNetwork = false, hasInternetCapability = false, isValidated = false)
        assertEquals(InternetReachability.OFFLINE, runtime.state.value.connectivity.internet)
    }

    private class RecordingForegroundController : ForegroundController {
        var started = false
        var stopped = false
        override fun start() { started = true }
        override fun stop() { stopped = true }
    }
}

package jp.co.soracom.qlm29hrtk.ui.settings

import jp.co.soracom.qlm29hrtk.AppState
import jp.co.soracom.qlm29hrtk.AppDisplayState
import jp.co.soracom.qlm29hrtk.AppUsbState
import jp.co.soracom.qlm29hrtk.AppNtripState
import jp.co.soracom.qlm29hrtk.AppSoracomState
import jp.co.soracom.qlm29hrtk.AppSmartphoneState
import jp.co.soracom.qlm29hrtk.AppTrackingState
import jp.co.soracom.qlm29hrtk.AppDiagnosticsState
import jp.co.soracom.qlm29hrtk.UsbConnectionState
import jp.co.soracom.qlm29hrtk.NtripConnectionState
import jp.co.soracom.qlm29hrtk.SmartphoneGnssStatus
import jp.co.soracom.qlm29hrtk.SoracomPublicationState
import jp.co.soracom.qlm29hrtk.nmea.NmeaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUiStateTest {
    @Test fun projectionGroupsConfigurationAndRuntimeStatusByFeature() {
        val result = SettingsUiState.from(
            AppState(
                display = AppDisplayState(darkTheme = true, keepScreenOn = true),
                smartphone = AppSmartphoneState(
                    enabled = true,
                    background = true,
                    status = SmartphoneGnssStatus.RECORDING,
                    pointCount = 12,
                ),
                usb = AppUsbState(connection = UsbConnectionState.CONNECTED, autoConnect = false),
                ntrip = AppNtripState(host = "caster.example", connection = NtripConnectionState.RECONNECTING),
                soracom = AppSoracomState(enabled = true, status = SoracomPublicationState.SUCCESS),
                tracking = AppTrackingState(pointCount = 34, selectedSessionId = "past-session"),
            ),
        )

        assertTrue(result.display.darkTheme)
        assertTrue(result.display.keepScreenOn)
        assertTrue(result.smartphone.enabled)
        assertTrue(result.smartphone.background)
        assertEquals("Recording", result.smartphone.status)
        assertEquals(12, result.smartphone.pointCount)
        assertEquals("Connected", result.usb.connection)
        assertFalse(result.usb.autoConnect)
        assertEquals("caster.example", result.ntrip.host)
        assertEquals("Reconnecting", result.ntrip.connection)
        assertTrue(result.soracom.enabled)
        assertEquals("Success", result.soracom.status)
        assertEquals(34, result.storage.qlmPointCount)
        assertEquals("past-session", result.sessions.selectedMapSessionId)
    }

    @Test fun projectionKeepsDiagnosticsInTheirOwnState() {
        val result = SettingsUiState.from(
            AppState(
                diagnostics = AppDiagnosticsState(
                    checksumErrors = 2,
                    ggaParseErrors = 3,
                    sentenceCounts = mapOf(NmeaType.GGA to 7L),
                ),
                ntrip = AppNtripState(reconnectCount = 4),
                soracom = AppSoracomState(successCount = 5, failureCount = 6),
            ),
        )

        assertEquals(2, result.diagnostics.checksumErrors)
        assertEquals(3, result.diagnostics.ggaParseErrors)
        assertEquals(4, result.diagnostics.ntripReconnectCount)
        assertEquals(5, result.diagnostics.soracomSuccessCount)
        assertEquals(6, result.diagnostics.soracomFailureCount)
        assertEquals(7L, result.diagnostics.sentenceCounts[NmeaType.GGA])
    }
}

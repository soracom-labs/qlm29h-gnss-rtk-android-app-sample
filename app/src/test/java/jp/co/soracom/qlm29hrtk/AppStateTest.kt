package jp.co.soracom.qlm29hrtk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    @Test fun usbConnectionStateDefinesRuntimeSemanticsAndDisplayLabels() {
        assertFalse(UsbConnectionState.DISCONNECTED.isActive)
        assertTrue(UsbConnectionState.CONNECTING.isActive)
        assertTrue(UsbConnectionState.CONNECTED.isActive)
        assertFalse(UsbConnectionState.ERROR.isActive)
        assertEquals("Connected", UsbConnectionState.CONNECTED.label)
    }

    @Test fun ntripConnectionStateSeparatesConnectedFromActiveSession() {
        assertFalse(NtripConnectionState.DISCONNECTED.hasActiveSession)
        assertTrue(NtripConnectionState.CONNECTING.hasActiveSession)
        assertTrue(NtripConnectionState.RECONNECTING.hasActiveSession)
        assertTrue(NtripConnectionState.CONNECTED.hasActiveSession)
        assertTrue(NtripConnectionState.CONNECTED.isConnected)
        assertFalse(NtripConnectionState.RECONNECTING.isConnected)
        assertFalse(NtripConnectionState.AUTH_ERROR.hasActiveSession)
        assertEquals("Auth Error", NtripConnectionState.AUTH_ERROR.label)
    }

    @Test fun remainingRuntimeStatusesHaveStableDisplaySemantics() {
        assertEquals("Receiving", RtcmStreamState.RECEIVING.label)
        assertTrue(SoracomPublicationState.VALIDATING.isBusy)
        assertTrue(SoracomPublicationState.SENDING.isBusy)
        assertFalse(SoracomPublicationState.SUCCESS.isBusy)
        assertEquals("Permission required", SmartphoneGnssStatus.PERMISSION_REQUIRED.label)
        assertEquals("Waiting for GPS", SmartphoneGnssStatus.WAITING_FOR_GPS.label)
        assertEquals("", SettingsPersistenceState.IDLE.label)
        assertEquals("Saving", SettingsPersistenceState.SAVING.label)
        assertEquals("12 mount points", SourceTableStatus.Loaded(12).label)
    }

    @Test fun displayCopyDoesNotReplaceUnrelatedRuntimeState() {
        val initial = AppState(usb = AppUsbState(connection = UsbConnectionState.CONNECTED), tracking = AppTrackingState(pointCount = 42))
        val changed = initial.copy(display = initial.display.copy(keepScreenOn = true))

        assertTrue(changed.display.keepScreenOn)
        assertFalse(changed.display.darkTheme)
        assertEquals(UsbConnectionState.CONNECTED, changed.usb.connection)
        assertEquals(42, changed.tracking.pointCount)
    }

    @Test fun usbCopyDoesNotReplaceDisplayOrTrackingState() {
        val initial = AppState(display = AppDisplayState(darkTheme = true), tracking = AppTrackingState(pointCount = 42))
        val changed = initial.copy(usb = initial.usb.copy(connection = UsbConnectionState.CONNECTED, receivedBytes = 10))

        assertEquals(UsbConnectionState.CONNECTED, changed.usb.connection)
        assertEquals(10, changed.usb.receivedBytes)
        assertTrue(changed.display.darkTheme)
        assertEquals(42, changed.tracking.pointCount)
    }

    @Test fun ntripCopyDoesNotReplaceUsbOrLatestFixState() {
        val initial = AppState(usb = AppUsbState(connection = UsbConnectionState.CONNECTED))
        val changed = initial.copy(ntrip = initial.ntrip.copy(connection = NtripConnectionState.RECONNECTING, reconnectCount = 2))

        assertEquals(NtripConnectionState.RECONNECTING, changed.ntrip.connection)
        assertEquals(2, changed.ntrip.reconnectCount)
        assertEquals(UsbConnectionState.CONNECTED, changed.usb.connection)
    }

    @Test fun soracomCopyDoesNotReplaceNtripOrUsbState() {
        val initial = AppState(
            usb = AppUsbState(connection = UsbConnectionState.CONNECTED),
            ntrip = AppNtripState(connection = NtripConnectionState.CONNECTED),
        )
        val changed = initial.copy(
            soracom = initial.soracom.copy(enabled = true, status = SoracomPublicationState.SUCCESS),
        )

        assertTrue(changed.soracom.enabled)
        assertEquals(SoracomPublicationState.SUCCESS, changed.soracom.status)
        assertEquals(UsbConnectionState.CONNECTED, changed.usb.connection)
        assertEquals(NtripConnectionState.CONNECTED, changed.ntrip.connection)
    }

    @Test fun smartphoneCopyRemainsIsolatedFromCorrectionAndPublicationState() {
        val initial = AppState(
            ntrip = AppNtripState(connection = NtripConnectionState.CONNECTED),
            soracom = AppSoracomState(enabled = true),
        )
        val changed = initial.copy(
            smartphone = initial.smartphone.copy(enabled = true, status = SmartphoneGnssStatus.RECORDING),
        )

        assertTrue(changed.smartphone.enabled)
        assertEquals(SmartphoneGnssStatus.RECORDING, changed.smartphone.status)
        assertEquals(NtripConnectionState.CONNECTED, changed.ntrip.connection)
        assertTrue(changed.soracom.enabled)
    }

    @Test fun selectingPastQlmSessionDoesNotChangeSmartphoneState() {
        val initial = AppState(
            smartphone = AppSmartphoneState(enabled = true, status = SmartphoneGnssStatus.RECORDING),
        )
        val changed = initial.copy(
            tracking = initial.tracking.copy(selectedSessionId = "past", follow = true),
        )

        assertEquals("past", changed.tracking.selectedSessionId)
        assertTrue(changed.tracking.follow)
        assertTrue(changed.smartphone.enabled)
        assertEquals(SmartphoneGnssStatus.RECORDING, changed.smartphone.status)
    }

    @Test fun clearingNoticeDoesNotClearDiagnostics() {
        val initial = AppState(
            diagnostics = AppDiagnosticsState(checksumErrors = 3),
            notice = AppNoticeState("Temporary error"),
        )
        val changed = initial.copy(notice = AppNoticeState())

        assertEquals(null, changed.notice.error)
        assertEquals(3, changed.diagnostics.checksumErrors)
    }

    @Test fun clearingConsoleDoesNotResetProtocolStates() {
        val initial = AppState(
            diagnostics = AppDiagnosticsState(checksumErrors = 2),
            usb = AppUsbState(connection = UsbConnectionState.CONNECTED),
            ntrip = AppNtripState(connection = NtripConnectionState.CONNECTED),
        )
        val changed = initial.copy(diagnostics = initial.diagnostics.copy(console = emptyList()))

        assertEquals(2, changed.diagnostics.checksumErrors)
        assertEquals(UsbConnectionState.CONNECTED, changed.usb.connection)
        assertEquals(NtripConnectionState.CONNECTED, changed.ntrip.connection)
    }
}

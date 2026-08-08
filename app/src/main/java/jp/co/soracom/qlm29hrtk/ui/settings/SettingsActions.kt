package jp.co.soracom.qlm29hrtk.ui.settings

import jp.co.soracom.qlm29hrtk.RtkRuntime
import jp.co.soracom.qlm29hrtk.soracom.SoracomQualityPolicy
import jp.co.soracom.qlm29hrtk.sessionlog.SessionLogExport

/** Commands exposed to Settings UI; no runtime state or lifecycle ownership. */
interface SettingsActions {
    fun showError(message: String)
    fun updateDarkTheme(value: Boolean)
    fun updateKeepScreenOn(value: Boolean)
    fun updateSmartphoneGnssBackground(value: Boolean)
    fun selectDevice(deviceId: Int)
    fun refreshDevices()
    fun updateUsbAutoConnect(value: Boolean)
    fun connect(deviceId: Int)
    fun disconnect()
    fun connectNtrip()
    fun disconnectNtrip()
    fun updateNtripHost(value: String)
    fun updateNtripPort(value: String)
    fun updateNtripMountPoint(value: String)
    fun updateNtripUsername(value: String)
    fun updateNtripPassword(value: String)
    fun saveNtripSettings()
    fun fetchSourceTable()
    fun chooseMountPoint(name: String)
    fun updateSoracomEnabled(value: Boolean)
    fun updateSoracomInterval(value: String)
    fun updateSoracomSendNoFix(value: Boolean)
    fun updateSoracomAllowNtripDisconnected(value: Boolean)
    fun updateSoracomQualityPolicy(value: SoracomQualityPolicy)
    fun sendSoracomNow()
    fun clearTracks()
    fun selectMapSession(sessionId: String?)
    fun deleteSession(sessionId: String)
    fun exportSessionLogs(sessionId: String, onComplete: (Result<SessionLogExport>) -> Unit)
}

class RtkSettingsActions(private val runtime: RtkRuntime) : SettingsActions {
    override fun showError(message: String) = runtime.showError(message)
    override fun updateDarkTheme(value: Boolean) = runtime.updateDarkTheme(value)
    override fun updateKeepScreenOn(value: Boolean) = runtime.updateKeepScreenOn(value)
    override fun updateSmartphoneGnssBackground(value: Boolean) = runtime.updateSmartphoneGnssBackground(value)
    override fun selectDevice(deviceId: Int) = runtime.selectDevice(deviceId)
    override fun refreshDevices() = runtime.refreshDevices().let { Unit }
    override fun updateUsbAutoConnect(value: Boolean) = runtime.updateUsbAutoConnect(value)
    override fun connect(deviceId: Int) = runtime.connect(deviceId).let { Unit }
    override fun disconnect() = runtime.disconnect().let { Unit }
    override fun connectNtrip() = runtime.connectNtrip()
    override fun disconnectNtrip() = runtime.disconnectNtrip()
    override fun updateNtripHost(value: String) = runtime.updateNtripHost(value)
    override fun updateNtripPort(value: String) = runtime.updateNtripPort(value)
    override fun updateNtripMountPoint(value: String) = runtime.updateNtripMountPoint(value)
    override fun updateNtripUsername(value: String) = runtime.updateNtripUsername(value)
    override fun updateNtripPassword(value: String) = runtime.updateNtripPassword(value)
    override fun saveNtripSettings() = runtime.saveNtripSettings()
    override fun fetchSourceTable() = runtime.fetchSourceTable()
    override fun chooseMountPoint(name: String) = runtime.chooseMountPoint(name)
    override fun updateSoracomEnabled(value: Boolean) = runtime.updateSoracomEnabled(value)
    override fun updateSoracomInterval(value: String) = runtime.updateSoracomInterval(value)
    override fun updateSoracomSendNoFix(value: Boolean) = runtime.updateSoracomSendNoFix(value)
    override fun updateSoracomAllowNtripDisconnected(value: Boolean) = runtime.updateSoracomAllowNtripDisconnected(value)
    override fun updateSoracomQualityPolicy(value: SoracomQualityPolicy) = runtime.updateSoracomQualityPolicy(value)
    override fun sendSoracomNow() = runtime.sendSoracomNow().let { Unit }
    override fun clearTracks() = runtime.clearTracks().let { Unit }
    override fun selectMapSession(sessionId: String?) = runtime.selectMapSession(sessionId)
    override fun deleteSession(sessionId: String) = runtime.deleteSession(sessionId).let { Unit }
    override fun exportSessionLogs(sessionId: String, onComplete: (Result<SessionLogExport>) -> Unit) =
        runtime.exportSessionLogs(sessionId, onComplete)
}

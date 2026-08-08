package jp.co.soracom.qlm29hrtk

import jp.co.soracom.qlm29hrtk.nmea.GgaFix
import jp.co.soracom.qlm29hrtk.nmea.GgaParser
import jp.co.soracom.qlm29hrtk.nmea.NmeaChecksum
import jp.co.soracom.qlm29hrtk.nmea.NmeaLineFramer
import jp.co.soracom.qlm29hrtk.nmea.ConsoleDirection
import jp.co.soracom.qlm29hrtk.nmea.ConsoleEntry
import jp.co.soracom.qlm29hrtk.nmea.NmeaType
import jp.co.soracom.qlm29hrtk.qlm29h.PqtmCommandBuilder
import jp.co.soracom.qlm29hrtk.ntrip.NtripClient
import jp.co.soracom.qlm29hrtk.ntrip.NtripDataSource
import jp.co.soracom.qlm29hrtk.ntrip.NtripSessionController
import jp.co.soracom.qlm29hrtk.ntrip.NtripSessionEvent
import jp.co.soracom.qlm29hrtk.ntrip.NtripConfig
import jp.co.soracom.qlm29hrtk.ntrip.RtcmInspector
import jp.co.soracom.qlm29hrtk.ntrip.MountPoint
import jp.co.soracom.qlm29hrtk.usb.SerialTransport
import jp.co.soracom.qlm29hrtk.usb.UsbSerialDevice
import jp.co.soracom.qlm29hrtk.usb.UsbSessionController
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import jp.co.soracom.qlm29hrtk.settings.NtripCredentials
import jp.co.soracom.qlm29hrtk.settings.SecureCredentialStore
import jp.co.soracom.qlm29hrtk.settings.SettingsRepository
import jp.co.soracom.qlm29hrtk.settings.StoredNtripSettings
import jp.co.soracom.qlm29hrtk.settings.SettingsValidator
import jp.co.soracom.qlm29hrtk.settings.SettingsValidationResult
import jp.co.soracom.qlm29hrtk.location.TrackRepository
import kotlinx.coroutines.flow.collectLatest
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import jp.co.soracom.qlm29hrtk.storage.SessionEntity
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import jp.co.soracom.qlm29hrtk.location.SmartphoneLocationProvider
import jp.co.soracom.qlm29hrtk.location.SmartphoneTrackRepository
import jp.co.soracom.qlm29hrtk.location.SmartphoneGnssPolicy
import jp.co.soracom.qlm29hrtk.location.SmartphoneLocationController
import jp.co.soracom.qlm29hrtk.soracom.NetworkTypeProvider
import jp.co.soracom.qlm29hrtk.soracom.PayloadBuilder
import jp.co.soracom.qlm29hrtk.soracom.SoracomSender
import jp.co.soracom.qlm29hrtk.soracom.SoracomQualityPolicy
import jp.co.soracom.qlm29hrtk.soracom.SoracomSendPolicy
import jp.co.soracom.qlm29hrtk.soracom.SoracomScheduleController
import jp.co.soracom.qlm29hrtk.soracom.SoracomSchedulePolicy
import jp.co.soracom.qlm29hrtk.service.ForegroundController
import jp.co.soracom.qlm29hrtk.sessionlog.QgnssSessionLogExporter
import jp.co.soracom.qlm29hrtk.sessionlog.SessionLogExport
import jp.co.soracom.qlm29hrtk.sessionlog.SessionRawLogStore

/**
 * Application-scoped owner of RTK I/O and persisted tracking state.
 *
 * This is deliberately not an Android ViewModel: the foreground service and
 * Activity share it across UI recreation, and its lifetime is the app process.
 * Controllers own individual long-running registrations and Jobs.
 */
class RtkRuntime(
    private val transport: SerialTransport,
    private val ntripClient: NtripDataSource = NtripClient(),
    private val settingsRepository: SettingsRepository? = null,
    private val credentialStore: SecureCredentialStore? = null,
    private val trackRepository: TrackRepository? = null,
    private val soracomSender: SoracomSender = SoracomSender(),
    private val networkTypeProvider: NetworkTypeProvider = NetworkTypeProvider { "Unknown" },
    private val foregroundController: ForegroundController? = null,
    private val smartphoneLocationProvider: SmartphoneLocationProvider? = null,
    private val smartphoneTrackRepository: SmartphoneTrackRepository? = null,
    private val sessionRawLogStore: SessionRawLogStore? = null,
    private val sessionLogExporter: QgnssSessionLogExporter? = null,
) {
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val framer = NmeaLineFramer()
    private val usbController = UsbSessionController(transport, runtimeScope)
    private val smartphoneController = SmartphoneLocationController(smartphoneLocationProvider)
    private var mapSessionJob: Job? = null
    private val mutableState = MutableStateFlow(AppState())
    val state = mutableState.asStateFlow()
    private val ntripController = NtripSessionController(ntripClient, runtimeScope)
    private var staleJob: Job? = null
    private val soracomScheduleController = SoracomScheduleController(runtimeScope)
    private val rtcmInspector = RtcmInspector()
    private var lastRtcmAtMillis: Long? = null
    private var smartphonePermissionGranted = false
    private var appInForeground = false

    init {
        trackRepository?.let { repository ->
            runtimeScope.launch {
                repository.pointCount.collectLatest { count ->
                    updateTrackingState { copy(pointCount = count) }
                }
            }
            runtimeScope.launch {
                repository.latestPoints.collectLatest { points ->
                    updateTrackingState { copy(livePoints = points) }
                }
            }
            runtimeScope.launch {
                repository.sessions.collectLatest { sessions ->
                    updateTrackingState { copy(sessions = sessions) }
                }
            }
        }
        smartphoneTrackRepository?.let { repository ->
            runtimeScope.launch { repository.latestPoints.collectLatest { points -> updateSmartphoneState { copy(points = points) } } }
            runtimeScope.launch { repository.pointCount.collectLatest { count -> updateSmartphoneState { copy(pointCount = count) } } }
        }
        runtimeScope.launch {
            runCatching {
                val settings = settingsRepository?.loadNtrip() ?: StoredNtripSettings()
                val credentials = credentialStore?.load() ?: NtripCredentials()
                mutableState.value = mutableState.value.copy(
                    ntrip = mutableState.value.ntrip.copy(
                        host = settings.host,
                        port = settings.port.toString(),
                        mountPoint = settings.mountPoint,
                        username = credentials.username,
                        password = credentials.password,
                    ),
                    // Runtime enablement is validated on every app launch.
                    soracom = mutableState.value.soracom.copy(
                        enabled = false,
                        intervalSeconds = settings.soracomIntervalSeconds.toString(),
                        status = SoracomPublicationState.DISABLED,
                        sendNoFix = settings.soracomSendNoFix,
                        allowNtripDisconnected = settings.soracomAllowNtripDisconnected,
                        qualityPolicy = runCatching { SoracomQualityPolicy.valueOf(settings.soracomQualityPolicy) }
                            .getOrDefault(SoracomQualityPolicy.ALL_VALID),
                    ),
                    display = AppDisplayState(
                        darkTheme = settings.darkTheme,
                        keepScreenOn = settings.keepScreenOn,
                    ),
                    // Active location capture is deliberately session-scoped. A
                    // process restart (including recovery from a crash) always
                    // returns to the safe Disabled state.
                    smartphone = mutableState.value.smartphone.copy(
                        enabled = false,
                        background = settings.smartphoneGnssBackground,
                        trackVisible = settings.smartphoneTrackVisible,
                        status = SmartphoneGnssStatus.DISABLED,
                    ),
                    usb = mutableState.value.usb.copy(autoConnect = settings.usbAutoConnect),
                )
                reconcileSmartphoneLocation()
            }.onFailure { mutableState.value = mutableState.value.copy(notice = AppNoticeState("Unable to load settings")) }
        }
        staleJob = runtimeScope.launch {
            while (true) {
                delay(1_000)
                val last = lastRtcmAtMillis
                if (last != null && System.currentTimeMillis() - last >= 10_000 && mutableState.value.ntrip.connection.isConnected) {
                    updateNtripState { copy(rtcmState = RtcmStreamState.STALE) }
                }
            }
        }
    }

    fun refreshDevices() = runtimeScope.launch {
        runCatching { usbController.listDevices() }
            .onSuccess { devices ->
                val current = mutableState.value
                mutableState.value = current.copy(
                    usb = current.usb.copy(
                        devices = devices,
                        selectedDeviceId = current.usb.selectedDeviceId?.takeIf { id -> devices.any { it.id == id } }
                            ?: devices.singleOrNull()?.id,
                    ),
                    notice = AppNoticeState(null),
                )
            }
            .onFailure { setError(it) }
    }

    fun selectDevice(deviceId: Int) {
        updateUsb { copy(selectedDeviceId = deviceId) }
    }

    fun showError(message: String) {
        mutableState.value = mutableState.value.copy(notice = AppNoticeState(message))
    }

    fun updateUsbAutoConnect(value: Boolean) {
        updateUsb { copy(autoConnect = value) }
        saveNtripSettings()
    }

    fun handleUsbAttached() = runtimeScope.launch {
        val devices = runCatching { usbController.listDevices() }.getOrElse {
            setError(it)
            return@launch
        }
        updateUsb { copy(devices = devices) }
        val device = devices.singleOrNull() ?: return@launch
        updateUsb { copy(selectedDeviceId = device.id) }
        if (mutableState.value.usb.autoConnect && usbController.hasPermission(device.id) && mutableState.value.usb.connection != UsbConnectionState.CONNECTED) {
            connect(device.id)
        }
    }

    fun connect(deviceId: Int) = runtimeScope.launch {
        val current = mutableState.value
        mutableState.value = current.copy(usb = current.usb.copy(connection = UsbConnectionState.CONNECTING), notice = AppNoticeState())
        val result = runCatching {
            usbController.connect(
                deviceId = deviceId,
                onBytes = ::acceptSerial,
                beforeRead = {
                    val sessionId = trackRepository?.startSession()
                    if (sessionId != null) sessionRawLogStore?.startSession(sessionId)
                },
            )
            updateUsb { copy(connection = UsbConnectionState.CONNECTED) }
            foregroundController?.start()
            send(PqtmCommandBuilder.enableRtk())
            startSoracomScheduler()
        }
        result.exceptionOrNull()?.let { error ->
            runCatching { usbController.disconnect() }
            runCatching { sessionRawLogStore?.finishSession() }
            runCatching { trackRepository?.endSession() }
            setError(error)
        }
    }

    fun disconnect() = disconnectInternal(stopForeground = true)

    private fun disconnectInternal(stopForeground: Boolean) = runtimeScope.launch {
        disconnectNtrip()
        stopSoracomScheduler()
        runCatching { usbController.disconnect() }
        runCatching { sessionRawLogStore?.finishSession() }
            .onFailure {
                mutableState.value = mutableState.value.copy(notice = AppNoticeState("Unable to finish session logs"))
            }
        runCatching { trackRepository?.endSession() }
        updateUsb { copy(connection = UsbConnectionState.DISCONNECTED) }
        if (stopForeground) foregroundController?.stop()
    }

    fun updateNtripHost(value: String) = updateNtrip { copy(host = value) }
    fun updateNtripPort(value: String) = updateNtrip { copy(port = value.filter(Char::isDigit)) }
    fun updateNtripMountPoint(value: String) = updateNtrip { copy(mountPoint = value) }
    fun updateNtripUsername(value: String) = updateNtrip { copy(username = value) }
    fun updateNtripPassword(value: String) = updateNtrip { copy(password = value) }
    fun updateSoracomEnabled(value: Boolean) {
        if (!value) {
            val current = mutableState.value
            mutableState.value = current.copy(soracom = current.soracom.copy(enabled = false, status = SoracomPublicationState.DISABLED), notice = AppNoticeState())
            stopSoracomScheduler()
            saveNtripSettings()
            return
        }
        val snapshot = mutableState.value
        if (snapshot.usb.connection != UsbConnectionState.CONNECTED) {
            mutableState.value = snapshot.copy(soracom = snapshot.soracom.copy(enabled = false, status = SoracomPublicationState.DISABLED), notice = AppNoticeState("Connect USB before enabling SORACOM"))
            return
        }
        if (snapshot.tracking.latestFix?.latitude == null || snapshot.tracking.latestFix.longitude == null) {
            mutableState.value = snapshot.copy(soracom = snapshot.soracom.copy(enabled = false, status = SoracomPublicationState.DISABLED), notice = AppNoticeState("Wait for a valid GNSS fix before enabling SORACOM"))
            return
        }
        mutableState.value = snapshot.copy(soracom = snapshot.soracom.copy(enabled = false, status = SoracomPublicationState.VALIDATING), notice = AppNoticeState())
        runtimeScope.launch {
            if (sendLatestToSoracom(ignorePolicy = true)) {
                val current = mutableState.value
                mutableState.value = current.copy(soracom = current.soracom.copy(enabled = true, status = SoracomPublicationState.SUCCESS), notice = AppNoticeState())
                startSoracomScheduler()
                saveNtripSettings()
            } else {
                updateSoracomState { copy(enabled = false, status = SoracomPublicationState.DISABLED) }
            }
        }
    }
    fun updateSoracomInterval(value: String) {
        val current = mutableState.value
        mutableState.value = current.copy(soracom = current.soracom.copy(intervalSeconds = value.filter(Char::isDigit)), notice = AppNoticeState())
        if (mutableState.value.soracom.enabled) startSoracomScheduler()
    }
    fun updateDarkTheme(value: Boolean) {
        updateDisplay { copy(darkTheme = value) }
    }
    fun updateKeepScreenOn(value: Boolean) {
        updateDisplay { copy(keepScreenOn = value) }
        saveNtripSettings()
    }
    private fun updateDisplay(block: AppDisplayState.() -> AppDisplayState) {
        val current = mutableState.value
        mutableState.value = current.copy(display = current.display.block())
    }
    private fun updateUsb(block: AppUsbState.() -> AppUsbState) {
        val current = mutableState.value
        mutableState.value = current.copy(usb = current.usb.block())
    }
    fun setSmartphonePermissionGranted(granted: Boolean) {
        smartphonePermissionGranted = granted
        if (!granted && mutableState.value.smartphone.enabled) {
            val current = mutableState.value
            mutableState.value = current.copy(
                smartphone = current.smartphone.copy(enabled = false, status = SmartphoneGnssStatus.PERMISSION_REQUIRED),
                notice = AppNoticeState("Precise location permission is required"),
            )
        }
        reconcileSmartphoneLocation()
    }
    fun setAppInForeground(foreground: Boolean) {
        appInForeground = foreground
        reconcileSmartphoneLocation()
    }
    fun updateSmartphoneGnssEnabled(enabled: Boolean) {
        if (!enabled) {
            val current = mutableState.value
            mutableState.value = current.copy(smartphone = current.smartphone.copy(enabled = false, status = SmartphoneGnssStatus.DISABLED), notice = AppNoticeState())
            smartphoneTrackRepository?.startNewSegment()
            reconcileSmartphoneLocation()
            saveNtripSettings()
            return
        }
        if (!smartphonePermissionGranted) {
            val current = mutableState.value
            mutableState.value = current.copy(
                smartphone = current.smartphone.copy(enabled = false, status = SmartphoneGnssStatus.PERMISSION_REQUIRED),
                notice = AppNoticeState("Precise location permission is required"),
            )
            return
        }
        if (!smartphoneController.providerEnabled) {
            val current = mutableState.value
            mutableState.value = current.copy(
                smartphone = current.smartphone.copy(enabled = false, status = SmartphoneGnssStatus.GPS_DISABLED),
                notice = AppNoticeState("Enable device location services before enabling Smartphone GNSS"),
            )
            return
        }
        val current = mutableState.value
        mutableState.value = current.copy(smartphone = current.smartphone.copy(enabled = true, status = SmartphoneGnssStatus.STARTING), notice = AppNoticeState())
        reconcileSmartphoneLocation()
        saveNtripSettings()
    }
    fun updateSmartphoneGnssBackground(enabled: Boolean) {
        updateSmartphoneState { copy(background = enabled) }
        reconcileSmartphoneLocation()
        saveNtripSettings()
    }
    fun updateSmartphoneTrackVisible(visible: Boolean) {
        updateSmartphoneState { copy(trackVisible = visible) }
        saveNtripSettings()
    }

    private fun reconcileSmartphoneLocation() {
        // SP-01/SP-03: phone location has its own lifecycle and repository. It
        // must never become an input to NTRIP or SORACOM, and every failure
        // returns capture to Disabled instead of allowing a restart loop.
        val state = mutableState.value
        val decision = SmartphoneGnssPolicy.decide(
            enabled = state.smartphone.enabled,
            permissionGranted = smartphonePermissionGranted,
            appInForeground = appInForeground,
            backgroundEnabled = state.smartphone.background,
        )
        val shouldRun = decision.shouldCapture
        if (shouldRun && !smartphoneController.isRunning) {
            smartphoneController.start { location ->
                    val current = mutableState.value
                    mutableState.value = current.copy(
                        smartphone = current.smartphone.copy(
                            status = SmartphoneGnssStatus.RECORDING,
                            lastLocationAt = Instant.ofEpochMilli(location.time).toString(),
                            accuracy = location.accuracy.takeIf { location.hasAccuracy() },
                        ),
                        notice = AppNoticeState(null),
                    )
                    runtimeScope.launch { runCatching { smartphoneTrackRepository?.record(location) }.onFailure { mutableState.value = mutableState.value.copy(notice = AppNoticeState("Unable to save smartphone location")) } }
                }
            .onSuccess {
                if (decision.requiresForegroundService) foregroundController?.start()
                updateSmartphoneState { copy(status = SmartphoneGnssStatus.WAITING_FOR_GPS) }
            }.onFailure {
                val current = mutableState.value
                mutableState.value = current.copy(
                    smartphone = current.smartphone.copy(enabled = false, status = SmartphoneGnssStatus.ERROR),
                    notice = AppNoticeState(it.message ?: "Unable to start Smartphone GNSS"),
                )
            }
        } else if (shouldRun && smartphoneController.isRunning && decision.requiresForegroundService) {
            foregroundController?.start()
        } else if (!shouldRun && smartphoneController.isRunning) {
            smartphoneController.stop()
            updateSmartphoneState {
                copy(status = if (state.smartphone.enabled) SmartphoneGnssStatus.PAUSED else SmartphoneGnssStatus.DISABLED)
            }
        }
        if (!state.smartphone.background && state.usb.connection == UsbConnectionState.DISCONNECTED) foregroundController?.stop()
    }
    fun updateSoracomSendNoFix(value: Boolean) = updateSoracomState { copy(sendNoFix = value) }
    fun updateSoracomAllowNtripDisconnected(value: Boolean) = updateSoracomState { copy(allowNtripDisconnected = value) }
    fun updateSoracomQualityPolicy(value: SoracomQualityPolicy) = updateSoracomState { copy(qualityPolicy = value) }

    private fun updateSoracomState(block: AppSoracomState.() -> AppSoracomState) {
        val current = mutableState.value
        mutableState.value = current.copy(soracom = current.soracom.block())
    }

    private fun updateSmartphoneState(block: AppSmartphoneState.() -> AppSmartphoneState) {
        val current = mutableState.value
        mutableState.value = current.copy(smartphone = current.smartphone.block())
    }

    private fun updateTrackingState(block: AppTrackingState.() -> AppTrackingState) {
        val current = mutableState.value
        mutableState.value = current.copy(tracking = current.tracking.block())
    }

    private fun updateNtrip(block: AppNtripState.() -> AppNtripState) {
        val current = mutableState.value
        mutableState.value = current.copy(ntrip = current.ntrip.block(), notice = AppNoticeState())
    }

    private fun updateNtripState(block: AppNtripState.() -> AppNtripState) {
        val current = mutableState.value
        mutableState.value = current.copy(ntrip = current.ntrip.block())
    }

    fun connectNtrip() {
        if (mutableState.value.usb.connection != UsbConnectionState.CONNECTED) {
            mutableState.value = mutableState.value.copy(notice = AppNoticeState("Connect USB before NTRIP"))
            return
        }
        val snapshot = mutableState.value
        val config = runCatching {
            NtripConfig(
                host = snapshot.ntrip.host.trim(),
                port = snapshot.ntrip.port.toIntOrNull() ?: error("NTRIP port is invalid"),
                mountPoint = snapshot.ntrip.mountPoint.trim(),
                username = snapshot.ntrip.username,
                password = snapshot.ntrip.password,
                tls = false,
            )
        }.getOrElse {
            val current = mutableState.value
            mutableState.value = current.copy(ntrip = current.ntrip.copy(connection = NtripConnectionState.ERROR), notice = AppNoticeState(it.message))
            return
        }
        ntripController.connect(
            config = config,
            latestGga = { mutableState.value.tracking.latestFix?.raw },
            onEvent = ::onNtripSessionEvent,
            onRtcm = ::forwardRtcm,
        )
    }

    fun onNetworkChanged() {
        updateSoracomState { copy(networkType = networkTypeProvider.current()) }
        if (mutableState.value.usb.connection == UsbConnectionState.CONNECTED && mutableState.value.ntrip.connection.hasActiveSession) {
            connectNtrip()
        }
    }

    fun fetchSourceTable() {
        val snapshot = mutableState.value
        val config = runCatching {
            NtripConfig(
                host = snapshot.ntrip.host.trim(),
                port = snapshot.ntrip.port.toIntOrNull() ?: error("NTRIP port is invalid"),
                mountPoint = snapshot.ntrip.mountPoint.ifBlank { "AUTO" },
                username = snapshot.ntrip.username,
                password = snapshot.ntrip.password,
                tls = false,
            )
        }.getOrElse {
            val current = mutableState.value
            mutableState.value = current.copy(ntrip = current.ntrip.copy(sourceTableState = SourceTableStatus.Error), notice = AppNoticeState(it.message))
            return
        }
        val current = mutableState.value
        mutableState.value = current.copy(ntrip = current.ntrip.copy(sourceTableState = SourceTableStatus.Loading), notice = AppNoticeState())
        runtimeScope.launch {
            runCatching { ntripClient.fetchSourceTable(config) }
                .onSuccess { points ->
                    val latest = mutableState.value
                    mutableState.value = latest.copy(
                        ntrip = latest.ntrip.copy(sourceTableState = SourceTableStatus.Loaded(points.size), mountPoints = points),
                        notice = AppNoticeState(null),
                    )
                }
                .onFailure {
                    val latest = mutableState.value
                    mutableState.value = latest.copy(ntrip = latest.ntrip.copy(sourceTableState = SourceTableStatus.Error), notice = AppNoticeState(it.message))
                }
        }
    }

    fun chooseMountPoint(name: String) {
        updateNtripState { copy(mountPoint = name) }
    }

    fun saveNtripSettings() {
        val snapshot = mutableState.value
        val validated = when (val result = SettingsValidator.validate(snapshot.ntrip.port, snapshot.soracom.intervalSeconds)) {
            is SettingsValidationResult.Valid -> result.values
            is SettingsValidationResult.Invalid -> {
                mutableState.value = snapshot.copy(ntrip = snapshot.ntrip.copy(settingsState = SettingsPersistenceState.ERROR), notice = AppNoticeState(result.message))
                return
            }
        }
        mutableState.value = snapshot.copy(ntrip = snapshot.ntrip.copy(settingsState = SettingsPersistenceState.SAVING), notice = AppNoticeState())
        runtimeScope.launch {
            runCatching {
                settingsRepository?.saveNtrip(
                    StoredNtripSettings(
                        host = snapshot.ntrip.host.trim(),
                        port = validated.ntripPort,
                        mountPoint = snapshot.ntrip.mountPoint.trim(),
                        soracomEnabled = snapshot.soracom.enabled,
                        soracomIntervalSeconds = validated.soracomIntervalSeconds,
                        darkTheme = snapshot.display.darkTheme,
                        keepScreenOn = snapshot.display.keepScreenOn,
                        smartphoneGnssEnabled = false,
                        smartphoneGnssBackground = snapshot.smartphone.background,
                        smartphoneTrackVisible = snapshot.smartphone.trackVisible,
                        usbAutoConnect = snapshot.usb.autoConnect,
                        soracomSendNoFix = snapshot.soracom.sendNoFix,
                        soracomAllowNtripDisconnected = snapshot.soracom.allowNtripDisconnected,
                        soracomQualityPolicy = snapshot.soracom.qualityPolicy.name,
                    ),
                )
                credentialStore?.save(NtripCredentials(snapshot.ntrip.username, snapshot.ntrip.password))
            }.onSuccess {
                val latest = mutableState.value
                mutableState.value = latest.copy(ntrip = latest.ntrip.copy(settingsState = SettingsPersistenceState.SAVED), notice = AppNoticeState())
            }.onFailure {
                val latest = mutableState.value
                mutableState.value = latest.copy(ntrip = latest.ntrip.copy(settingsState = SettingsPersistenceState.ERROR), notice = AppNoticeState("Unable to save settings"))
            }
        }
    }

    fun disconnectNtrip() {
        ntripController.disconnect()
        updateNtripState { copy(connection = NtripConnectionState.DISCONNECTED) }
    }

    private fun onNtripSessionEvent(event: NtripSessionEvent) {
        val current = mutableState.value
        mutableState.value = when (event) {
            NtripSessionEvent.Connecting -> current.copy(ntrip = current.ntrip.copy(connection = NtripConnectionState.CONNECTING), notice = AppNoticeState())
            NtripSessionEvent.Connected -> current.copy(ntrip = current.ntrip.copy(connection = NtripConnectionState.CONNECTED), notice = AppNoticeState())
            NtripSessionEvent.Disconnected -> current.copy(ntrip = current.ntrip.copy(connection = NtripConnectionState.DISCONNECTED, rtcmState = RtcmStreamState.NONE))
            is NtripSessionEvent.AuthError -> current.copy(ntrip = current.ntrip.copy(connection = NtripConnectionState.AUTH_ERROR), notice = AppNoticeState(event.message))
            is NtripSessionEvent.TlsError -> current.copy(ntrip = current.ntrip.copy(connection = NtripConnectionState.TLS_ERROR), notice = AppNoticeState(event.message))
            is NtripSessionEvent.Reconnecting -> current.copy(
                ntrip = current.ntrip.copy(
                    connection = NtripConnectionState.RECONNECTING,
                    rtcmState = RtcmStreamState.STALE,
                    reconnectCount = current.ntrip.reconnectCount + 1,
                ),
                notice = AppNoticeState(event.message),
            )
        }
    }

    private suspend fun forwardRtcm(bytes: ByteArray) {
        // DATA-06: preserve the caster input before forwarding. The byte stream
        // is not framed or re-encoded, matching QGNSS NTRIP_Client_Rece logs.
        sessionRawLogStore?.appendRtcm(bytes)
        usbController.write(bytes)
        lastRtcmAtMillis = System.currentTimeMillis()
        val messages = rtcmInspector.accept(bytes)
        mutableState.value = mutableState.value.let {
            it.copy(
                usb = it.usb.copy(
                    transmittedBytes = it.usb.transmittedBytes + bytes.size,
                    lastTransmittedAt = Instant.now().toString(),
                ),
                ntrip = it.ntrip.copy(
                    rtcmBytes = it.ntrip.rtcmBytes + bytes.size,
                    lastRtcmMessage = messages.lastOrNull()?.let { message -> "${message.id}: ${message.label}" }
                        ?: it.ntrip.lastRtcmMessage,
                    rtcmState = RtcmStreamState.RECEIVING,
                    lastRtcmReceivedAt = Instant.now().toString(),
                ),
            )
        }
    }

    fun onDeviceDetached() {
        disconnectInternal(stopForeground = false)
        refreshDevices()
    }

    private suspend fun send(bytes: ByteArray) {
        usbController.write(bytes)
        val text = bytes.toString(Charsets.US_ASCII).trimEnd('\r', '\n')
        mutableState.value = mutableState.value.let {
            it.copy(
                usb = it.usb.copy(
                    transmittedBytes = it.usb.transmittedBytes + bytes.size,
                    lastTransmittedAt = Instant.now().toString(),
                ),
                diagnostics = it.diagnostics.copy(
                    console = (it.diagnostics.console + ConsoleEntry(
                        direction = ConsoleDirection.TX,
                        type = NmeaType.detect(text),
                        text = text,
                        checksumValid = NmeaChecksum.isValid(text),
                    )).takeLast(10_000),
                ),
            )
        }
    }

    fun acceptSerial(bytes: ByteArray) {
        // DATA-06: this is the replayable source of truth. Console entries and
        // TrackPoint rows are projections and intentionally remain bounded.
        sessionRawLogStore?.appendNmea(bytes)
        val lines = framer.accept(bytes)
        if (lines.isEmpty()) {
            mutableState.value = mutableState.value.let {
                it.copy(usb = it.usb.copy(
                    receivedBytes = it.usb.receivedBytes + bytes.size,
                    lastReceivedAt = Instant.now().toString(),
                ))
            }
            return
        }
        lines.forEachIndexed { index, line ->
            val valid = NmeaChecksum.isValid(line)
            val fix = if (valid) GgaParser.parse(line) else null
            val type = NmeaType.detect(line)
            mutableState.value = mutableState.value.let { current ->
                current.copy(
                    tracking = current.tracking.copy(latestFix = fix ?: current.tracking.latestFix),
                    diagnostics = current.diagnostics.copy(
                        checksumErrors = current.diagnostics.checksumErrors + if (valid) 0 else 1,
                        ggaParseErrors = current.diagnostics.ggaParseErrors + if (valid && type == NmeaType.GGA && fix == null) 1 else 0,
                        sentenceCounts = current.diagnostics.sentenceCounts +
                            (type to ((current.diagnostics.sentenceCounts[type] ?: 0L) + 1)),
                        console = (current.diagnostics.console + ConsoleEntry(
                            direction = ConsoleDirection.RX,
                            type = type,
                            text = line,
                            checksumValid = valid,
                        )).takeLast(10_000),
                    ),
                    usb = current.usb.copy(
                        receivedBytes = current.usb.receivedBytes + if (index == 0) bytes.size else 0,
                        lastReceivedAt = if (index == 0) Instant.now().toString() else current.usb.lastReceivedAt,
                    ),
                )
            }
            if (fix != null) {
                runtimeScope.launch {
                    runCatching {
                        trackRepository?.record(
                            fix = fix,
                            ntripConnected = mutableState.value.ntrip.connection.isConnected,
                            lastRtcmReceivedAt = mutableState.value.ntrip.lastRtcmReceivedAt,
                        )
                    }.onFailure { mutableState.value = mutableState.value.copy(notice = AppNoticeState("Unable to save track point")) }
                }
            }
        }
    }

    fun clearTracks() = runtimeScope.launch {
        runCatching {
            trackRepository?.clearAll()
            smartphoneTrackRepository?.clearAll()
        }
            .onFailure { mutableState.value = mutableState.value.copy(notice = AppNoticeState("Unable to clear tracks")) }
    }

    fun deleteSession(sessionId: String) = runtimeScope.launch {
        runCatching {
            if (mutableState.value.tracking.selectedSessionId == sessionId) selectMapSession(null)
            if (trackRepository?.deleteEndedSession(sessionId) == true) {
                sessionRawLogStore?.deleteSession(sessionId)
                withContext(Dispatchers.IO) { sessionLogExporter?.deleteExports(sessionId) }
            }
        }
            .onFailure { mutableState.value = mutableState.value.copy(notice = AppNoticeState("Unable to delete session")) }
    }

    fun exportSessionLogs(sessionId: String, onComplete: (Result<SessionLogExport>) -> Unit) {
        runtimeScope.launch {
            val result = runCatching {
                val session = mutableState.value.tracking.sessions.firstOrNull { it.id == sessionId }
                    ?: error("Session not found")
                check(session.endedAt != null) { "Disconnect USB before exporting the active session" }
                val exporter = sessionLogExporter ?: error("Historical log export is unavailable")
                val points = trackRepository?.loadSessionPoints(sessionId).orEmpty()
                withContext(Dispatchers.IO) { exporter.export(session, points) }
            }
            result.exceptionOrNull()?.let { error ->
                mutableState.value = mutableState.value.copy(notice = AppNoticeState(error.message ?: "Unable to export session log"))
            }
            onComplete(result)
        }
    }

    fun selectMapSession(sessionId: String?) {
        // MAP-03/DATA-03: a selected historical session is a stable snapshot
        // source for the map. Live emissions continue to be recorded but must
        // not replace the user's explicit historical selection.
        mapSessionJob?.cancel()
        mapSessionJob = null
        if (sessionId == null) {
            updateTrackingState { copy(selectedSessionId = null, selectedSessionPoints = emptyList(), follow = true) }
            return
        }
        updateTrackingState { copy(selectedSessionId = sessionId, selectedSessionPoints = emptyList(), follow = true) }
        mapSessionJob = runtimeScope.launch {
            trackRepository?.sessionPoints(sessionId)?.collectLatest { points ->
                if (mutableState.value.tracking.selectedSessionId == sessionId) {
                    updateTrackingState { copy(selectedSessionPoints = points) }
                }
            }
        }
    }

    fun clearConsole() {
        val current = mutableState.value
        mutableState.value = current.copy(diagnostics = current.diagnostics.copy(console = emptyList()))
    }

    fun setMapFollow(enabled: Boolean) {
        updateTrackingState { copy(follow = enabled) }
    }

    fun sendSoracomNow() = runtimeScope.launch { sendLatestToSoracom() }

    private fun startSoracomScheduler() {
        soracomScheduleController.stop()
        val snapshot = mutableState.value
        val intervalMillis = SoracomSchedulePolicy.intervalMillis(
            enabled = snapshot.soracom.enabled,
            usbConnected = snapshot.usb.connection == UsbConnectionState.CONNECTED,
            seconds = snapshot.soracom.intervalSeconds,
        ) ?: return
        soracomScheduleController.start(intervalMillis) { sendLatestToSoracom() }
    }

    private fun stopSoracomScheduler() {
        soracomScheduleController.stop()
        updateSoracomState {
            copy(status = if (enabled) SoracomPublicationState.IDLE else SoracomPublicationState.DISABLED)
        }
    }

    private suspend fun sendLatestToSoracom(ignorePolicy: Boolean = false): Boolean {
        val snapshot = mutableState.value
        val fix = snapshot.tracking.latestFix ?: return false
        val ntripConnected = snapshot.ntrip.connection.isConnected
        val policy = SoracomSendPolicy(
            sendNoFix = snapshot.soracom.sendNoFix,
            allowNtripDisconnected = snapshot.soracom.allowNtripDisconnected,
            qualityPolicy = snapshot.soracom.qualityPolicy,
        )
        if (!ignorePolicy && !policy.allows(fix.quality, ntripConnected)) return false
        val payload = PayloadBuilder.build(
            fix = fix,
            ntripConnected = ntripConnected,
            rtcmAgeSec = lastRtcmAtMillis?.let { (System.currentTimeMillis() - it).coerceAtLeast(0) / 1_000.0 },
        ) ?: return false
        mutableState.value = snapshot.copy(
            soracom = snapshot.soracom.copy(status = SoracomPublicationState.SENDING, networkType = networkTypeProvider.current()),
        )
        val result = soracomSender.send(payload)
        mutableState.value = mutableState.value.let { current ->
            current.copy(
                soracom = current.soracom.copy(
                    status = if (result.successful) SoracomPublicationState.SUCCESS else SoracomPublicationState.FAILED,
                    lastSentAt = Instant.now().toString(),
                    lastHttpStatus = result.httpStatus,
                    failureCount = current.soracom.failureCount + if (result.successful) 0 else 1,
                    successCount = current.soracom.successCount + if (result.successful) 1 else 0,
                ),
                notice = AppNoticeState(if (result.successful) null else "SORACOM: ${result.message}"),
            )
        }
        return result.successful
    }

    private fun setError(error: Throwable) {
        val current = mutableState.value
        mutableState.value = current.copy(
            usb = current.usb.copy(connection = UsbConnectionState.ERROR),
            notice = AppNoticeState(error.message ?: error.javaClass.simpleName),
        )
    }

    /** Releases process-scoped resources; intended for tests or app shutdown. */
    fun shutdown() {
        ntripController.disconnect()
        staleJob?.cancel()
        soracomScheduleController.stop()
        usbController.stopReading()
        smartphoneController.stop()
        sessionRawLogStore?.close()
        runtimeScope.cancel()
    }
}

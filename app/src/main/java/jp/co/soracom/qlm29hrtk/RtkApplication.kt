package jp.co.soracom.qlm29hrtk

import android.app.Application
import jp.co.soracom.qlm29hrtk.location.TrackRepository
import jp.co.soracom.qlm29hrtk.location.AndroidSmartphoneLocationProvider
import jp.co.soracom.qlm29hrtk.location.SmartphoneTrackRepository
import jp.co.soracom.qlm29hrtk.network.AndroidConnectivityMonitor
import jp.co.soracom.qlm29hrtk.service.AndroidForegroundController
import jp.co.soracom.qlm29hrtk.settings.AndroidSecureCredentialStore
import jp.co.soracom.qlm29hrtk.settings.AndroidSettingsRepository
import jp.co.soracom.qlm29hrtk.soracom.AndroidNetworkTypeProvider
import jp.co.soracom.qlm29hrtk.soracom.SoracomSender
import jp.co.soracom.qlm29hrtk.storage.AppDatabase
import jp.co.soracom.qlm29hrtk.sessionlog.QgnssSessionLogExporter
import jp.co.soracom.qlm29hrtk.sessionlog.SessionRawLogStore
import jp.co.soracom.qlm29hrtk.usb.AndroidUsbSerialTransport
import java.io.File
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

class RtkApplication : Application() {
    lateinit var runtime: RtkRuntime
        private set
    private lateinit var connectivityMonitor: AndroidConnectivityMonitor

    override fun onCreate() {
        super.onCreate()
        // MapLibre storage/offline APIs require global SDK configuration before
        // OfflineManager or MapView is created.
        MapLibre.getInstance(this, "", WellKnownTileServer.MapLibre)
        val database = AppDatabase.get(this)
        val rawLogStore = SessionRawLogStore(File(filesDir, "session_logs"))
        runtime = RtkRuntime(
            transport = AndroidUsbSerialTransport(this),
            settingsRepository = AndroidSettingsRepository(this),
            credentialStore = AndroidSecureCredentialStore(this),
            trackRepository = TrackRepository(database.trackDao()),
            soracomSender = SoracomSender(),
            networkTypeProvider = AndroidNetworkTypeProvider(this),
            foregroundController = AndroidForegroundController(this),
            smartphoneLocationProvider = AndroidSmartphoneLocationProvider(this),
            smartphoneTrackRepository = SmartphoneTrackRepository(database.smartphoneTrackDao()),
            sessionRawLogStore = rawLogStore,
            sessionLogExporter = QgnssSessionLogExporter(rawLogStore, File(filesDir, "nmea_logs")),
        )
        connectivityMonitor = AndroidConnectivityMonitor(this) { status ->
            runtime.onNetworkStatusChanged(
                hasNetwork = status.hasNetwork,
                hasInternetCapability = status.hasInternetCapability,
                isValidated = status.isValidated,
            )
        }
        connectivityMonitor.start()
    }
}

package jp.co.soracom.qlm29hrtk.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import jp.co.soracom.qlm29hrtk.ntrip.NtripDefaults
import jp.co.soracom.qlm29hrtk.location.TrackRetentionPolicy
import jp.co.soracom.qlm29hrtk.soracom.SoracomSchedulePolicy

data class StoredNtripSettings(
    val host: String = NtripDefaults.HOST,
    val port: Int = NtripDefaults.PORT,
    val mountPoint: String = NtripDefaults.MOUNT_POINT,
    val soracomEnabled: Boolean = false,
    val soracomIntervalSeconds: Int = SoracomSchedulePolicy.DEFAULT_INTERVAL_SECONDS,
    val darkTheme: Boolean = false,
    val keepScreenOn: Boolean = false,
    val smartphoneGnssEnabled: Boolean = false,
    val smartphoneGnssBackground: Boolean = false,
    val smartphoneTrackVisible: Boolean = true,
    val usbAutoConnect: Boolean = true,
    val soracomSendNoFix: Boolean = false,
    val soracomAllowNtripDisconnected: Boolean = true,
    val soracomQualityPolicy: String = "ALL_VALID",
    val trackPointLimit: Int = TrackRetentionPolicy.DEFAULT_MAX_POINTS,
)

interface SettingsRepository {
    suspend fun loadNtrip(): StoredNtripSettings
    suspend fun saveNtrip(settings: StoredNtripSettings)
    suspend fun saveTrackPointLimit(value: Int)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AndroidSettingsRepository(context: Context) : SettingsRepository {
    private val appContext = context.applicationContext

    override suspend fun loadNtrip(): StoredNtripSettings {
        val values = appContext.settingsDataStore.data.first()
        return StoredNtripSettings(
            // NTRIP-06: repair legacy blank values without replacing a custom caster.
            host = values[HOST]?.takeIf(String::isNotBlank) ?: NtripDefaults.HOST,
            port = values[PORT] ?: NtripDefaults.PORT,
            mountPoint = values[MOUNT_POINT]?.takeIf(String::isNotBlank) ?: NtripDefaults.MOUNT_POINT,
            soracomEnabled = values[SORACOM_ENABLED] ?: false,
            // SORACOM-05: legacy values predate the cost warning and must be reconfirmed.
            soracomIntervalSeconds = SoracomIntervalPersistencePolicy.restore(
                savedSeconds = values[SORACOM_INTERVAL],
                savedVersion = values[SORACOM_INTERVAL_POLICY_VERSION],
            ),
            darkTheme = values[DARK_THEME] ?: false,
            keepScreenOn = values[KEEP_SCREEN_ON] ?: false,
            smartphoneGnssEnabled = values[SMARTPHONE_GNSS_ENABLED] ?: false,
            smartphoneGnssBackground = values[SMARTPHONE_GNSS_BACKGROUND] ?: false,
            smartphoneTrackVisible = values[SMARTPHONE_TRACK_VISIBLE] ?: true,
            usbAutoConnect = values[USB_AUTO_CONNECT] ?: true,
            soracomSendNoFix = values[SORACOM_SEND_NO_FIX] ?: false,
            soracomAllowNtripDisconnected = values[SORACOM_ALLOW_NTRIP_DISCONNECTED] ?: true,
            soracomQualityPolicy = values[SORACOM_QUALITY_POLICY] ?: "ALL_VALID",
            trackPointLimit = TrackRetentionPolicy.normalize(values[TRACK_POINT_LIMIT]),
        )
    }

    override suspend fun saveNtrip(settings: StoredNtripSettings) {
        appContext.settingsDataStore.edit { values ->
            values[HOST] = settings.host
            values[PORT] = settings.port
            values[MOUNT_POINT] = settings.mountPoint
            values[SORACOM_ENABLED] = settings.soracomEnabled
            values[SORACOM_INTERVAL] = settings.soracomIntervalSeconds
            values[SORACOM_INTERVAL_POLICY_VERSION] = SoracomIntervalPersistencePolicy.VERSION
            values[DARK_THEME] = settings.darkTheme
            values[KEEP_SCREEN_ON] = settings.keepScreenOn
            values[SMARTPHONE_GNSS_ENABLED] = settings.smartphoneGnssEnabled
            values[SMARTPHONE_GNSS_BACKGROUND] = settings.smartphoneGnssBackground
            values[SMARTPHONE_TRACK_VISIBLE] = settings.smartphoneTrackVisible
            values[USB_AUTO_CONNECT] = settings.usbAutoConnect
            values[SORACOM_SEND_NO_FIX] = settings.soracomSendNoFix
            values[SORACOM_ALLOW_NTRIP_DISCONNECTED] = settings.soracomAllowNtripDisconnected
            values[SORACOM_QUALITY_POLICY] = settings.soracomQualityPolicy
            values[TRACK_POINT_LIMIT] = TrackRetentionPolicy.normalize(settings.trackPointLimit)
        }
    }

    override suspend fun saveTrackPointLimit(value: Int) {
        require(TrackRetentionPolicy.isAllowed(value))
        appContext.settingsDataStore.edit { values -> values[TRACK_POINT_LIMIT] = value }
    }

    private companion object {
        val HOST = stringPreferencesKey("ntrip_host")
        val PORT = intPreferencesKey("ntrip_port")
        val MOUNT_POINT = stringPreferencesKey("ntrip_mount_point")
        val SORACOM_ENABLED = booleanPreferencesKey("soracom_enabled")
        val SORACOM_INTERVAL = intPreferencesKey("soracom_interval_seconds")
        val SORACOM_INTERVAL_POLICY_VERSION = intPreferencesKey("soracom_interval_policy_version")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SMARTPHONE_GNSS_ENABLED = booleanPreferencesKey("smartphone_gnss_enabled")
        val SMARTPHONE_GNSS_BACKGROUND = booleanPreferencesKey("smartphone_gnss_background")
        val SMARTPHONE_TRACK_VISIBLE = booleanPreferencesKey("smartphone_track_visible")
        val USB_AUTO_CONNECT = booleanPreferencesKey("usb_auto_connect")
        val SORACOM_SEND_NO_FIX = booleanPreferencesKey("soracom_send_no_fix")
        val SORACOM_ALLOW_NTRIP_DISCONNECTED = booleanPreferencesKey("soracom_allow_ntrip_disconnected")
        val SORACOM_QUALITY_POLICY = stringPreferencesKey("soracom_quality_policy")
        val TRACK_POINT_LIMIT = intPreferencesKey("track_point_limit")
    }
}

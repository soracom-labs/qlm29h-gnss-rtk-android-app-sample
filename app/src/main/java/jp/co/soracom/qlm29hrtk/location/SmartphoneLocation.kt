package jp.co.soracom.qlm29hrtk.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackDao
import jp.co.soracom.qlm29hrtk.storage.SmartphoneTrackPointEntity
import java.util.UUID

fun interface SmartphoneLocationListener { fun onLocation(location: Location) }

interface SmartphoneLocationProvider {
    val providerEnabled: Boolean
    fun start(listener: SmartphoneLocationListener)
    fun stop()
}

class AndroidSmartphoneLocationProvider(context: Context) : SmartphoneLocationProvider, LocationListener {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(LocationManager::class.java)
    private var listener: SmartphoneLocationListener? = null
    override val providerEnabled: Boolean get() = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    @SuppressLint("MissingPermission")
    override fun start(listener: SmartphoneLocationListener) {
        stop()
        this.listener = listener
        val request = LocationRequest.Builder(1_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, request, appContext.mainExecutor, this)
    }

    override fun stop() {
        manager.removeUpdates(this)
        listener = null
    }

    override fun onLocationChanged(location: Location) { listener?.onLocation(location) }
}

class SmartphoneTrackRepository(private val dao: SmartphoneTrackDao) {
    val latestPoints = dao.observeLatest(2_000)
    val pointCount = dao.observeCount()
    private var segmentId = UUID.randomUUID().toString()
    private var lastTimestamp = 0L

    suspend fun record(location: Location) {
        val timestamp = location.time.takeIf { it > 0 } ?: System.currentTimeMillis()
        if (lastTimestamp == 0L || timestamp - lastTimestamp > SEGMENT_GAP_MILLIS) segmentId = UUID.randomUUID().toString()
        dao.insert(
            SmartphoneTrackPointEntity(
                segmentId = segmentId,
                timestamp = timestamp,
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude.takeIf { location.hasAltitude() },
                accuracy = location.accuracy.takeIf { location.hasAccuracy() },
                speed = location.speed.takeIf { location.hasSpeed() },
                bearing = location.bearing.takeIf { location.hasBearing() },
                provider = location.provider.orEmpty(),
            ),
        )
        lastTimestamp = timestamp
        dao.deleteOlderThan(timestamp - RETENTION_MILLIS)
        dao.trimToNewest(MAX_POINTS)
    }

    fun startNewSegment() { lastTimestamp = 0L }
    suspend fun clearAll() { dao.clearAll(); startNewSegment() }

    companion object {
        const val MAX_POINTS = 50_000
        const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1_000
        const val SEGMENT_GAP_MILLIS = 10_000L
    }
}

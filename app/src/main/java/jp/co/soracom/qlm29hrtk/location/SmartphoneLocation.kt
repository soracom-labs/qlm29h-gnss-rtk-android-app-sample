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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private var maxPoints = TrackRetentionPolicy.DEFAULT_MAX_POINTS
    private var retainedPointCount: Int? = null
    private val mutex = Mutex()

    suspend fun record(location: Location) = mutex.withLock {
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
        val count = retainedPointCount?.plus(1) ?: dao.countPoints()
        retainedPointCount = prune(count)
    }

    fun startNewSegment() { lastTimestamp = 0L }
    suspend fun clearAll() = mutex.withLock {
        dao.clearAll()
        retainedPointCount = 0
        startNewSegment()
    }

    suspend fun updateMaxPoints(value: Int) = mutex.withLock {
        require(TrackRetentionPolicy.isAllowed(value))
        maxPoints = value
        retainedPointCount = prune(retainedPointCount ?: dao.countPoints())
    }

    private suspend fun prune(currentCount: Int): Int {
        val excess = TrackRetentionPolicy.excessPointCount(currentCount, maxPoints)
        if (excess == 0) return currentCount
        return currentCount - dao.deleteOldestPoints(excess)
    }

    companion object {
        const val SEGMENT_GAP_MILLIS = 10_000L
    }
}

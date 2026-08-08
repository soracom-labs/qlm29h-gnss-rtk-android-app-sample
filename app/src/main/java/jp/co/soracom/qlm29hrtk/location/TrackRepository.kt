package jp.co.soracom.qlm29hrtk.location

import jp.co.soracom.qlm29hrtk.nmea.GgaFix
import jp.co.soracom.qlm29hrtk.storage.SessionEntity
import jp.co.soracom.qlm29hrtk.storage.TrackDao
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrackRepository(private val dao: TrackDao) {
    val pointCount: Flow<Int> = dao.observePointCount()
    val latestPoints: Flow<List<TrackPointEntity>> = dao.observeLatestPoints(2_000)
    val sessions: Flow<List<SessionEntity>> = dao.observeSessions()
    fun sessionPoints(sessionId: String): Flow<List<TrackPointEntity>> = dao.observeSessionPoints(sessionId)
    suspend fun loadSessionPoints(sessionId: String): List<TrackPointEntity> = dao.loadSessionPoints(sessionId)
    private var sessionId: String? = null
    private var lastSavedAt = 0L
    private val mutex = Mutex()

    suspend fun startSession(): String = mutex.withLock { startSessionLocked() }

    private suspend fun startSessionLocked(): String {
        sessionId?.let { return it }
        return UUID.randomUUID().toString().also { id ->
            dao.insertSession(SessionEntity(id = id, startedAt = System.currentTimeMillis()))
            sessionId = id
            lastSavedAt = 0L
        }
    }

    suspend fun endSession() = mutex.withLock {
        val id = sessionId ?: return
        dao.endSession(id, System.currentTimeMillis())
        sessionId = null
        lastSavedAt = 0L
    }

    suspend fun record(fix: GgaFix, ntripConnected: Boolean, lastRtcmReceivedAt: String?): Boolean = mutex.withLock {
        val latitude = fix.latitude ?: return false
        val longitude = fix.longitude ?: return false
        val now = System.currentTimeMillis()
        if (now - lastSavedAt < MAP_INTERVAL_MILLIS) return false
        val id = sessionId ?: startSessionLocked()
        dao.insertAndCount(
            TrackPointEntity(
                sessionId = id,
                timestamp = now,
                latitude = latitude,
                longitude = longitude,
                altitude = fix.altitude,
                quality = fix.quality,
                qualityLabel = fix.qualityLabel,
                satellites = fix.satellites,
                hdop = fix.hdop,
                ntripConnected = ntripConnected,
                lastRtcmReceivedAt = lastRtcmReceivedAt,
                rawGga = fix.raw,
            ),
        )
        lastSavedAt = now
        prune(now)
        return true
    }

    suspend fun clearAll() = mutex.withLock { dao.clearPointsAndResetCounts() }
    suspend fun deleteEndedSession(sessionId: String): Boolean = mutex.withLock { dao.deleteEndedSession(sessionId) > 0 }

    private suspend fun prune(now: Long) {
        // DATA-02: retention is a storage policy. It is intentionally separate
        // from the smaller Live Map query limit above (DATA-03).
        dao.deleteOlderThan(TrackRetentionPolicy.cutoff(now))
        dao.trimToNewest(TrackRetentionPolicy.maxPoints)
    }

    companion object {
        const val MAX_POINTS = 50_000
        const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1_000
        const val MAP_INTERVAL_MILLIS = 1_000L
    }
}

object TrackRetentionPolicy {
    const val maxPoints = TrackRepository.MAX_POINTS
    fun cutoff(now: Long): Long = now - TrackRepository.RETENTION_MILLIS
}

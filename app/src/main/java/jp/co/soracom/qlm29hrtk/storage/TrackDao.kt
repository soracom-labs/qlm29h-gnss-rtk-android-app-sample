package jp.co.soracom.qlm29hrtk.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SessionEntity)

    @Insert
    suspend fun insertPoint(point: TrackPointEntity)

    @Query("UPDATE sessions SET endedAt = :endedAt WHERE id = :sessionId")
    suspend fun endSession(sessionId: String, endedAt: Long)

    @Query(
        """UPDATE sessions SET
            pointCount = pointCount + 1,
            rtkFixedCount = rtkFixedCount + CASE WHEN :quality = 4 THEN 1 ELSE 0 END,
            rtkFloatCount = rtkFloatCount + CASE WHEN :quality = 5 THEN 1 ELSE 0 END,
            spsCount = spsCount + CASE WHEN :quality = 1 THEN 1 ELSE 0 END,
            dgpsCount = dgpsCount + CASE WHEN :quality = 2 THEN 1 ELSE 0 END,
            deadReckoningCount = deadReckoningCount + CASE WHEN :quality = 6 THEN 1 ELSE 0 END
            WHERE id = :sessionId""",
    )
    suspend fun incrementSession(sessionId: String, quality: Int)

    @Query("SELECT COUNT(*) FROM track_points")
    fun observePointCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM track_points")
    suspend fun countPoints(): Int

    @Query("SELECT * FROM track_points ORDER BY timestamp DESC LIMIT :limit")
    fun observeLatestPoints(limit: Int): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp DESC, id DESC")
    fun observeSessionPoints(sessionId: String): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
    suspend fun loadSessionPoints(sessionId: String): List<TrackPointEntity>

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("DELETE FROM track_points WHERE id IN (SELECT id FROM track_points ORDER BY timestamp ASC, id ASC LIMIT :count)")
    suspend fun deleteOldestPoints(count: Int): Int

    @Query("DELETE FROM track_points WHERE sessionId = :sessionId")
    suspend fun deleteSessionPoints(sessionId: String)

    @Query("DELETE FROM track_points")
    suspend fun deleteAllPoints()

    @Query("DELETE FROM sessions WHERE id = :sessionId AND endedAt IS NOT NULL")
    suspend fun deleteEndedSession(sessionId: String): Int

    @Query("""UPDATE sessions SET pointCount = 0, rtkFixedCount = 0, rtkFloatCount = 0,
        spsCount = 0, dgpsCount = 0, deadReckoningCount = 0""")
    suspend fun resetSessionCounts()

    @Transaction
    suspend fun clearPointsAndResetCounts() {
        deleteAllPoints()
        resetSessionCounts()
    }

    @Transaction
    suspend fun insertAndCount(point: TrackPointEntity) {
        insertPoint(point)
        incrementSession(point.sessionId, point.quality)
    }
}

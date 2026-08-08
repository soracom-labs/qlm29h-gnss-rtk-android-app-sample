package jp.co.soracom.qlm29hrtk.storage

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "smartphone_track_points",
    indices = [Index("timestamp"), Index("segmentId")],
)
data class SmartphoneTrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val segmentId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val bearing: Float?,
    val provider: String,
)

@Dao
interface SmartphoneTrackDao {
    @Insert suspend fun insert(point: SmartphoneTrackPointEntity)
    @Query("SELECT * FROM smartphone_track_points ORDER BY timestamp DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<SmartphoneTrackPointEntity>>
    @Query("SELECT COUNT(*) FROM smartphone_track_points")
    fun observeCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM smartphone_track_points")
    suspend fun countPoints(): Int
    @Query("DELETE FROM smartphone_track_points WHERE id IN (SELECT id FROM smartphone_track_points ORDER BY timestamp ASC, id ASC LIMIT :count)")
    suspend fun deleteOldestPoints(count: Int): Int
    @Query("DELETE FROM smartphone_track_points")
    suspend fun clearAll()
}

package jp.co.soracom.qlm29hrtk.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val pointCount: Int = 0,
    val rtkFixedCount: Int = 0,
    val rtkFloatCount: Int = 0,
    val spsCount: Int = 0,
    val dgpsCount: Int = 0,
    val deadReckoningCount: Int = 0,
)

@Entity(
    tableName = "track_points",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId"), Index("timestamp")],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val quality: Int,
    val qualityLabel: String,
    val satellites: Int?,
    val hdop: Double?,
    val ntripConnected: Boolean,
    val lastRtcmReceivedAt: String?,
    val rawGga: String?,
)

package jp.co.soracom.qlm29hrtk.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SessionEntity::class, TrackPointEntity::class, SmartphoneTrackPointEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun smartphoneTrackDao(): SmartphoneTrackDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "qlm29h.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `smartphone_track_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `segmentId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `altitude` REAL, `accuracy` REAL, `speed` REAL, `bearing` REAL, `provider` TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smartphone_track_points_timestamp` ON `smartphone_track_points` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smartphone_track_points_segmentId` ON `smartphone_track_points` (`segmentId`)")
            }
        }

        /** MAP-03/SP-06: preserve legacy SP points while enabling exact future association. */
        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `smartphone_track_points` ADD COLUMN `qlmSessionId` TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_smartphone_track_points_qlmSessionId` ON `smartphone_track_points` (`qlmSessionId`)")
            }
        }
    }
}

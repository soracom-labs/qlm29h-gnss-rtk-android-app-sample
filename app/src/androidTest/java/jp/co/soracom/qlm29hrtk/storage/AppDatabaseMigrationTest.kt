package jp.co.soracom.qlm29hrtk.storage

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val databaseName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate2To3PreservesSmartphonePointsAndAddsSessionAssociation() {
        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                """INSERT INTO smartphone_track_points
                    (id, segmentId, timestamp, latitude, longitude, altitude, accuracy, speed, bearing, provider)
                    VALUES (1, 'legacy-segment', 1000, 0.0, 0.0, NULL, 3.0, 0.0, NULL, 'gps')""",
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 3, true, AppDatabase.MIGRATION_2_3).apply {
            query("SELECT segmentId, qlmSessionId FROM smartphone_track_points WHERE id = 1").use { cursor ->
                cursor.moveToFirst()
                assertEquals("legacy-segment", cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
            }
            close()
        }
    }
}

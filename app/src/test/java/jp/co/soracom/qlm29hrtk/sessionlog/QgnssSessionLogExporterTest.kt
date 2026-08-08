package jp.co.soracom.qlm29hrtk.sessionlog

import java.nio.file.Files
import jp.co.soracom.qlm29hrtk.storage.SessionEntity
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class QgnssSessionLogExporterTest {
    @Test fun data06ExportsRawNmeaAndRtcmWithoutTransformation() {
        val root = Files.createTempDirectory("qgnss-export").toFile()
        val store = SessionRawLogStore(root.resolve("raw"))
        try {
            val session = SessionEntity("session-raw", startedAt = 1_700_000_000_000)
            val nmea = "\$GNGGA,raw*00\r\n\$GNRMC,raw*00\r\n".toByteArray()
            val rtcm = byteArrayOf(0xD3.toByte(), 0x00, 0x01, 0x23)
            store.nmeaFile(session.id).parentFile?.mkdirs()
            store.nmeaFile(session.id).writeBytes(nmea)
            store.rtcmFile(session.id).writeBytes(rtcm)

            val result = QgnssSessionLogExporter(store, root.resolve("exports")).export(session, emptyList())

            assertEquals(NmeaExportSource.RAW_SESSION, result.nmeaSource)
            assertEquals(2, result.files.size)
            assertArrayEquals(nmea, result.files[0].readBytes())
            assertArrayEquals(rtcm, result.files[1].readBytes())
        } finally {
            store.close()
            root.deleteRecursively()
        }
    }

    @Test fun data06OlderSessionFallsBackToChronologicalGgaWithCrLf() {
        val root = Files.createTempDirectory("qgnss-gga-fallback").toFile()
        val store = SessionRawLogStore(root.resolve("raw"))
        try {
            val session = SessionEntity("session-old", startedAt = 1_700_000_000_000)
            val newer = point(2, 2_000, "\$GNGGA,newer*00")
            val older = point(1, 1_000, "\$GNGGA,older*00\n")

            val result = QgnssSessionLogExporter(store, root.resolve("exports"))
                .export(session, listOf(newer, older))

            assertEquals(NmeaExportSource.GGA_FALLBACK, result.nmeaSource)
            assertEquals(
                "\$GNGGA,older*00\r\n\$GNGGA,newer*00\r\n",
                result.files.single().readText(Charsets.US_ASCII),
            )
        } finally {
            store.close()
            root.deleteRecursively()
        }
    }

    private fun point(id: Long, timestamp: Long, rawGga: String) = TrackPointEntity(
        id = id,
        sessionId = "session-old",
        timestamp = timestamp,
        latitude = 0.0,
        longitude = 0.0,
        altitude = null,
        quality = 1,
        qualityLabel = "SPS",
        satellites = null,
        hdop = null,
        ntripConnected = false,
        lastRtcmReceivedAt = null,
        rawGga = rawGga,
    )
}

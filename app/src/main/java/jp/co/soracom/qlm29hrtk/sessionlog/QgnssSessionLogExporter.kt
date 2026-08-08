package jp.co.soracom.qlm29hrtk.sessionlog

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import jp.co.soracom.qlm29hrtk.storage.SessionEntity
import jp.co.soracom.qlm29hrtk.storage.TrackPointEntity

data class SessionLogExport(
    val files: List<File>,
    val nmeaSource: NmeaExportSource,
)

enum class NmeaExportSource { RAW_SESSION, GGA_FALLBACK }

/** Builds QGNSS-compatible files without mixing app metadata into replay bytes. */
class QgnssSessionLogExporter(
    private val rawLogStore: SessionRawLogStore,
    private val exportDirectory: File,
) {
    /** Caller must run this blocking file operation on an I/O dispatcher. */
    fun export(session: SessionEntity, points: List<TrackPointEntity>): SessionLogExport {
        require(SAFE_SESSION_ID.matches(session.id)) { "Invalid session id" }
        val sessionExportDirectory = File(exportDirectory, session.id).apply { mkdirs() }
        val stamp = FILE_STAMP.format(Instant.ofEpochMilli(session.startedAt).atZone(ZoneId.systemDefault()))
        val suffix = session.id.take(8)
        val nmeaTarget = File(sessionExportDirectory, "QLM29H-ANDROID-${stamp}_$suffix.log")
        val rawNmea = rawLogStore.nmeaFile(session.id)
        val source = if (rawNmea.isFile && rawNmea.length() > 0L) {
            rawNmea.copyTo(nmeaTarget, overwrite = true)
            NmeaExportSource.RAW_SESSION
        } else {
            writeGgaFallback(nmeaTarget, points)
            NmeaExportSource.GGA_FALLBACK
        }

        val files = mutableListOf(nmeaTarget)
        val rawRtcm = rawLogStore.rtcmFile(session.id)
        if (rawRtcm.isFile && rawRtcm.length() > 0L) {
            val rtcmTarget = File(sessionExportDirectory, "NTRIP_Client_Rece${stamp}_$suffix.log")
            rawRtcm.copyTo(rtcmTarget, overwrite = true)
            files += rtcmTarget
        }
        return SessionLogExport(files, source)
    }

    /** Removes prepared copies; the raw source is owned by SessionRawLogStore. */
    fun deleteExports(sessionId: String) {
        require(SAFE_SESSION_ID.matches(sessionId)) { "Invalid session id" }
        File(exportDirectory, sessionId).deleteRecursively()
    }

    private fun writeGgaFallback(target: File, points: List<TrackPointEntity>) {
        val sentences = points.asSequence()
            .sortedWith(compareBy<TrackPointEntity> { it.timestamp }.thenBy { it.id })
            .mapNotNull { it.rawGga?.trimEnd('\r', '\n')?.takeIf(String::isNotBlank) }
            .toList()
        check(sentences.isNotEmpty()) { "This session has no replayable NMEA data" }
        target.outputStream().buffered().use { output ->
            sentences.forEach { sentence ->
                output.write(sentence.toByteArray(Charsets.US_ASCII))
                output.write(CRLF)
            }
        }
    }

    companion object {
        private val FILE_STAMP = DateTimeFormatter.ofPattern("MMdd_HHmmss")
        private val CRLF = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte())
        private val SAFE_SESSION_ID = Regex("[A-Za-z0-9_-]+")
    }
}

package jp.co.soracom.qlm29hrtk.nmea

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object ConsoleLogExporter {
    fun save(context: Context, entries: List<ConsoleEntry>): File {
        val directory = File(context.filesDir, "nmea_logs").apply { mkdirs() }
        val stamp = FILE_STAMP.format(Instant.now())
        val file = File(directory, "nmea_$stamp.txt")
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            entries.forEach { entry ->
                writer.append(entry.timestamp)
                writer.append(' ')
                writer.append(entry.direction.name)
                writer.append(' ')
                writer.append(if (entry.checksumValid) "OK" else "CHECKSUM_ERROR")
                writer.append(' ')
                writer.appendLine(entry.text)
            }
        }
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share NMEA log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private val FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC)
}

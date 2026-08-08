package jp.co.soracom.qlm29hrtk.storage

import android.content.Context
import java.io.File

object StorageInspector {
    fun nmeaLogBytes(context: Context): Long = LOG_DIRECTORIES.sumOf { name -> sizeOf(File(context.filesDir, name)) }
    fun clearNmeaLogs(context: Context): Int {
        return LOG_DIRECTORIES.sumOf { name -> clearFiles(File(context.filesDir, name)) }
    }
    fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
    private fun sizeOf(file: File): Long = when {
        file.isFile -> file.length()
        file.isDirectory -> file.listFiles().orEmpty().sumOf(::sizeOf)
        else -> 0
    }

    private fun clearFiles(file: File): Int = when {
        file.isFile -> if (file.delete()) 1 else 0
        file.isDirectory -> file.listFiles().orEmpty().sumOf(::clearFiles)
        else -> 0
    }

    private val LOG_DIRECTORIES = listOf("nmea_logs", "session_logs")
}

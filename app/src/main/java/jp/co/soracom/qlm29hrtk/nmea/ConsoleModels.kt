package jp.co.soracom.qlm29hrtk.nmea

import java.time.Instant

enum class ConsoleDirection { RX, TX }

enum class NmeaType {
    GGA, RMC, GSA, GSV, VTG, GLL, ZDA, GST, PQTM, OTHER;

    companion object {
        fun detect(line: String): NmeaType {
            val address = line.trimStart('$').substringBefore(',').substringBefore('*')
            if (address.startsWith("PQTM")) return PQTM
            return entries.firstOrNull { it != PQTM && it != OTHER && address.endsWith(it.name) } ?: OTHER
        }
    }
}

data class ConsoleEntry(
    val timestamp: String = Instant.now().toString(),
    val direction: ConsoleDirection,
    val type: NmeaType,
    val text: String,
    val checksumValid: Boolean,
)

package jp.co.soracom.qlm29hrtk.ntrip

object SourceTableParser {
    fun parse(text: String): List<MountPoint> = text.lineSequence()
        .map(String::trim)
        .filter { it.startsWith("STR;") }
        .mapNotNull(::parseStream)
        .toList()

    private fun parseStream(line: String): MountPoint? {
        val fields = line.split(';')
        val name = fields.getOrNull(1)?.takeIf(String::isNotBlank) ?: return null
        return MountPoint(
            name = name,
            identifier = fields.getOrNull(2)?.ifBlank { null },
            format = fields.getOrNull(3)?.ifBlank { null },
            carrier = fields.getOrNull(5)?.toIntOrNull(),
            latitude = fields.getOrNull(9)?.toDoubleOrNull(),
            longitude = fields.getOrNull(10)?.toDoubleOrNull(),
            requiresAuthentication = fields.getOrNull(15)?.equals("B", ignoreCase = true) == true,
            raw = line,
        )
    }
}

package jp.co.soracom.qlm29hrtk.nmea

object GgaParser {
    fun parse(sentence: String): GgaFix? {
        if (!NmeaChecksum.isValid(sentence)) return null
        val body = sentence.trim().substringAfter('$').substringBefore('*')
        val fields = body.split(',')
        if (!fields.firstOrNull().orEmpty().endsWith("GGA") || fields.size < 15) return null
        val quality = fields[6].toIntOrNull() ?: return null
        return GgaFix(
            utc = fields[1],
            latitude = coordinate(fields[2], fields[3], latitude = true),
            longitude = coordinate(fields[4], fields[5], latitude = false),
            quality = quality,
            qualityLabel = FixQuality.labelFor(quality),
            satellites = fields[7].toIntOrNull(),
            hdop = fields[8].toDoubleOrNull(),
            altitude = fields[9].toDoubleOrNull(),
            geoidSeparation = fields[11].toDoubleOrNull(),
            differentialAge = fields[13].toDoubleOrNull(),
            differentialStationId = fields[14].ifBlank { null },
            raw = sentence.trim(),
        )
    }

    internal fun coordinate(value: String, hemisphere: String, latitude: Boolean): Double? {
        val degreeDigits = if (latitude) 2 else 3
        if (value.length <= degreeDigits) return null
        val degrees = value.take(degreeDigits).toDoubleOrNull() ?: return null
        val minutes = value.drop(degreeDigits).toDoubleOrNull() ?: return null
        if (minutes !in 0.0..<60.0) return null
        val result = degrees + minutes / 60.0
        val valid = if (latitude) hemisphere in setOf("N", "S") && result <= 90 else hemisphere in setOf("E", "W") && result <= 180
        if (!valid) return null
        return if (hemisphere == "S" || hemisphere == "W") -result else result
    }
}

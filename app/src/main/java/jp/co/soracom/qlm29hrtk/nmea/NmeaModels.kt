package jp.co.soracom.qlm29hrtk.nmea

enum class FixQuality(val value: Int, val label: String) {
    NO_FIX(0, "No Fix"),
    GPS_SPS(1, "GPS SPS"),
    DGPS_SBAS(2, "DGPS / SBAS"),
    RTK_FIXED(4, "RTK Fixed"),
    RTK_FLOAT(5, "RTK Float"),
    DEAD_RECKONING(6, "Dead Reckoning");

    companion object {
        fun labelFor(value: Int) = entries.firstOrNull { it.value == value }?.label ?: "Unknown($value)"
    }
}

data class GgaFix(
    val utc: String,
    val latitude: Double?,
    val longitude: Double?,
    val quality: Int,
    val qualityLabel: String,
    val satellites: Int?,
    val hdop: Double?,
    val altitude: Double?,
    val geoidSeparation: Double?,
    val differentialAge: Double?,
    val differentialStationId: String?,
    val raw: String,
)

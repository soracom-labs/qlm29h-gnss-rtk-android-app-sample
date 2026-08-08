package jp.co.soracom.qlm29hrtk.soracom

import jp.co.soracom.qlm29hrtk.nmea.GgaFix
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
data class SoracomPayload(
    val timestamp: String,
    val lat: Double,
    val lon: Double,
    val quality: Int,
    val alt: Double? = null,
    @SerialName("quality_label") val qualityLabel: String? = null,
    val satellites: Int? = null,
    val hdop: Double? = null,
    @SerialName("geoid_separation") val geoidSeparation: Double? = null,
    @SerialName("differential_age") val differentialAge: Double? = null,
    @SerialName("differential_station_id") val differentialStationId: String? = null,
    @SerialName("raw_gga") val rawGga: String? = null,
    @SerialName("ntrip_connected") val ntripConnected: Boolean? = null,
    @SerialName("rtcm_age_sec") val rtcmAgeSec: Double? = null,
)

object PayloadBuilder {
    private val json = Json { explicitNulls = false }

    fun build(fix: GgaFix, ntripConnected: Boolean, rtcmAgeSec: Double?): SoracomPayload? {
        val lat = fix.latitude ?: return null
        val lon = fix.longitude ?: return null
        return SoracomPayload(
            timestamp = Instant.now().toString(),
            lat = lat,
            lon = lon,
            quality = fix.quality,
            alt = fix.altitude,
            qualityLabel = fix.qualityLabel,
            satellites = fix.satellites,
            hdop = fix.hdop,
            geoidSeparation = fix.geoidSeparation,
            differentialAge = fix.differentialAge,
            differentialStationId = fix.differentialStationId,
            rawGga = null,
            ntripConnected = ntripConnected,
            rtcmAgeSec = rtcmAgeSec,
        )
    }

    fun encode(payload: SoracomPayload): String = json.encodeToString(payload)
}

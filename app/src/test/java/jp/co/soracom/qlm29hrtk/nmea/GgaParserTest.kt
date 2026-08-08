package jp.co.soracom.qlm29hrtk.nmea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GgaParserTest {
    private val sentence = "\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"

    @Test fun parsesGga() {
        val fix = GgaParser.parse(sentence)!!
        assertEquals(48.1173, fix.latitude!!, 0.000001)
        assertEquals(11.5166667, fix.longitude!!, 0.000001)
        assertEquals("GPS SPS", fix.qualityLabel)
        assertEquals(8, fix.satellites)
    }

    @Test fun rejectsInvalidChecksum() = assertNull(GgaParser.parse(sentence.replace("4807.038", "4807.039")))
    @Test fun mapsUnknownQuality() = assertEquals("Unknown(3)", FixQuality.labelFor(3))
    @Test fun appliesSouthernAndWesternSigns() {
        assertEquals(-35.5, GgaParser.coordinate("3530.0", "S", true)!!, 0.0)
        assertEquals(-139.5, GgaParser.coordinate("13930.0", "W", false)!!, 0.0)
    }
}

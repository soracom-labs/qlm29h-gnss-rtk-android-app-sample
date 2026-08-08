package jp.co.soracom.qlm29hrtk.nmea

import org.junit.Assert.assertEquals
import org.junit.Test

class ConsoleModelsTest {
    @Test fun detectsStandardTalkerSentenceTypes() {
        assertEquals(NmeaType.GGA, NmeaType.detect("\$GNGGA,123519,...*00"))
        assertEquals(NmeaType.RMC, NmeaType.detect("\$GPRMC,123519,...*00"))
        assertEquals(NmeaType.GSV, NmeaType.detect("\$GLGSV,1,1,...*00"))
    }

    @Test fun detectsPqtmAndUnknown() {
        assertEquals(NmeaType.PQTM, NmeaType.detect("\$PQTMCFGRTK,OK*00"))
        assertEquals(NmeaType.OTHER, NmeaType.detect("noise"))
    }
}

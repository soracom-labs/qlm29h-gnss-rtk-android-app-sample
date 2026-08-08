package jp.co.soracom.qlm29hrtk.nmea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NmeaChecksumTest {
    @Test fun validatesKnownSentence() = assertTrue(NmeaChecksum.isValid("\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"))
    @Test fun rejectsCorruption() = assertFalse(NmeaChecksum.isValid("\$GPGGA,123519,4807.039,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47"))
    @Test fun buildsPqtmCommand() = assertEquals("\$PQTMCFGRTK,W,1,1*6C\r\n", NmeaChecksum.append("PQTMCFGRTK,W,1,1"))
}

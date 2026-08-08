package jp.co.soracom.qlm29hrtk.nmea

import org.junit.Assert.assertEquals
import org.junit.Test

class NmeaLineFramerTest {
    @Test fun framesAcrossChunks() {
        val framer = NmeaLineFramer()
        assertEquals(emptyList<String>(), framer.accept("\$GPG".toByteArray()))
        assertEquals(listOf("\$GPGGA,1*00", "\$GPRMC,2*00"), framer.accept("GA,1*00\r\n\$GPRMC,2*00\n".toByteArray()))
    }
}

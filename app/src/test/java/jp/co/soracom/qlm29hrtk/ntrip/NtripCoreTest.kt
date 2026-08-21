package jp.co.soracom.qlm29hrtk.ntrip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NtripCoreTest {
    @Test fun soracomGuideDefaultsRequestTheAutoMountPoint() {
        val config = NtripConfig()
        val request = NtripRequestBuilder.stream(config).toString(Charsets.US_ASCII)

        assertEquals("qrtksa1.quectel.com", config.host)
        assertEquals(2101, config.port)
        assertEquals("AUTO", config.mountPoint)
        assertTrue(request.startsWith("GET /AUTO HTTP/1.0\r\n"))
    }

    @Test fun acceptsIcyAndHttpSuccess() {
        assertEquals(NtripConnectResult.Success, NtripResponseParser.classify("ICY 200 OK"))
        assertEquals(NtripConnectResult.Success, NtripResponseParser.classify("HTTP/1.1 200 OK"))
        assertTrue(NtripResponseParser.classify("HTTP/1.1 401 Unauthorized") is NtripConnectResult.Failure)
        assertEquals(401, NtripResponseParser.statusCode("ICY 401 Unauthorized"))
        assertEquals(503, NtripResponseParser.statusCode("HTTP/1.1 503 Service Unavailable"))
        assertTrue(NtripResponseParser.isSourceTableSuccess("SOURCETABLE 200 OK"))
    }

    @Test fun buildsAuthenticatedRequestWithoutLeakingIntoOtherFields() {
        val request = NtripRequestBuilder.stream(NtripConfig("caster.example", mountPoint = "/MOUNT", username = "user", password = "pass"))
            .toString(Charsets.US_ASCII)
        assertTrue(request.startsWith("GET /MOUNT HTTP/1.0\r\n"))
        assertTrue(request.contains("Authorization: Basic dXNlcjpwYXNz\r\n"))
        assertFalse(request.contains("user:pass"))
    }

    @Test fun parsesSourceTableStreams() {
        val table = "SOURCETABLE 200 OK\r\nSTR;TOKYO;Tokyo;RTCM 3.2;1005(10),1074(1);2;GPS;SNIP;JPN;35.6;139.7;1;0;Generator;none;B;N;9600;misc\r\nENDSOURCETABLE\r\n"
        val point = SourceTableParser.parse(table).single()
        assertEquals("TOKYO", point.name)
        assertEquals("RTCM 3.2", point.format)
        assertEquals(35.6, point.latitude!!, 0.0)
        assertTrue(point.requiresAuthentication)
    }

    @Test fun inspectsFragmentedRtcmFrame() {
        val id = 1074
        val payload = byteArrayOf((id shr 4).toByte(), ((id and 0x0F) shl 4).toByte())
        val frame = byteArrayOf(0xD3.toByte(), 0, payload.size.toByte()) + payload + byteArrayOf(0, 0, 0)
        val inspector = RtcmInspector()
        assertTrue(inspector.accept(frame.copyOfRange(0, 4)).isEmpty())
        assertEquals(RtcmMessage(1074, "GPS MSM4"), inspector.accept(frame.copyOfRange(4, frame.size)).single())
    }
}

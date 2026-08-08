package jp.co.soracom.qlm29hrtk.soracom

import jp.co.soracom.qlm29hrtk.nmea.GgaParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoracomSenderTest {
    private val fix = GgaParser.parse("\$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47")!!

    @Test fun payloadAlwaysContainsRequiredFields() {
        val encoded = PayloadBuilder.encode(PayloadBuilder.build(fix, true, 0.4)!!)
        assertTrue(encoded.contains("\"lat\":48.1173"))
        assertTrue(encoded.contains("\"lon\":11.516666"))
        assertTrue(encoded.contains("\"quality\":1"))
        assertTrue(encoded.contains("\"timestamp\":"))
    }

    @Test fun failedPostIsAttemptedOnlyOnce() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            val sender = SoracomSender(OkHttpClient.Builder().retryOnConnectionFailure(false).build(), server.url("/").toString())
            val result = sender.send(PayloadBuilder.build(fix, false, null)!!)
            assertFalse(result.successful)
            assertEquals(503, result.httpStatus)
            assertEquals(1, server.requestCount)
        }
    }
}

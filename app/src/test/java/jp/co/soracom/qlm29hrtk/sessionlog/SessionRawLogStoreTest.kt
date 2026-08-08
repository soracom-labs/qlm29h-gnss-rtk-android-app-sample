package jp.co.soracom.qlm29hrtk.sessionlog

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class SessionRawLogStoreTest {
    @Test fun data06PreservesNmeaAndRtcmAsSeparateOrderedByteStreams() = runBlocking {
        val root = Files.createTempDirectory("session-raw-log").toFile()
        val store = SessionRawLogStore(root)
        try {
            store.startSession("session-1")
            store.appendNmea("\$GNGGA,one*00\r\n".toByteArray())
            store.appendNmea("\$GNRMC,two*00\r\n".toByteArray())
            val rtcm = byteArrayOf(0xD3.toByte(), 0x00, 0x01, 0x7F)
            store.appendRtcm(rtcm)
            store.finishSession()

            assertArrayEquals(
                "\$GNGGA,one*00\r\n\$GNRMC,two*00\r\n".toByteArray(),
                store.nmeaFile("session-1").readBytes(),
            )
            assertArrayEquals(rtcm, store.rtcmFile("session-1").readBytes())
        } finally {
            store.close()
            root.deleteRecursively()
        }
    }
}

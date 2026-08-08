package jp.co.soracom.qlm29hrtk.nmea

class NmeaLineFramer(private val maxBufferSize: Int = 16_384) {
    private val buffer = StringBuilder()

    @Synchronized
    fun accept(bytes: ByteArray): List<String> {
        buffer.append(bytes.toString(Charsets.US_ASCII))
        if (buffer.length > maxBufferSize) buffer.delete(0, buffer.length - maxBufferSize)
        val lines = mutableListOf<String>()
        while (true) {
            val newline = buffer.indexOf("\n")
            if (newline < 0) break
            val line = buffer.substring(0, newline).trimEnd('\r')
            buffer.delete(0, newline + 1)
            if (line.isNotEmpty()) lines += line
        }
        return lines
    }
}

package jp.co.soracom.qlm29hrtk.ntrip

data class RtcmMessage(val id: Int, val label: String)

class RtcmInspector {
    private var buffer = ByteArray(0)

    @Synchronized
    fun accept(chunk: ByteArray): List<RtcmMessage> {
        buffer += chunk
        val messages = mutableListOf<RtcmMessage>()
        var offset = 0
        while (buffer.size - offset >= 6) {
            if (buffer[offset].toInt() and 0xFF != 0xD3) {
                offset++
                continue
            }
            val payloadLength = ((buffer[offset + 1].toInt() and 0x03) shl 8) or (buffer[offset + 2].toInt() and 0xFF)
            val frameLength = 3 + payloadLength + 3
            if (buffer.size - offset < frameLength) break
            if (payloadLength >= 2) {
                val id = ((buffer[offset + 3].toInt() and 0xFF) shl 4) or ((buffer[offset + 4].toInt() and 0xF0) ushr 4)
                messages += RtcmMessage(id, label(id))
            }
            offset += frameLength
        }
        if (offset > 0) buffer = buffer.copyOfRange(offset, buffer.size)
        if (buffer.size > MAX_BUFFER) buffer = buffer.takeLast(MAX_BUFFER).toByteArray()
        return messages
    }

    companion object {
        private const val MAX_BUFFER = 4_096
        fun label(id: Int): String = when (id) {
            1005 -> "Reference station coordinates"
            1033 -> "Receiver / antenna description"
            1074 -> "GPS MSM4"
            1084 -> "GLONASS MSM4"
            1094 -> "Galileo MSM4"
            1124 -> "BDS MSM4"
            else -> "msg#$id"
        }
    }
}

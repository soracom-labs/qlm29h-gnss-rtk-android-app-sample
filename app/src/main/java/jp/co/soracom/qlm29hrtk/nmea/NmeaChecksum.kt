package jp.co.soracom.qlm29hrtk.nmea

object NmeaChecksum {
    fun calculate(payload: String): Int = payload.fold(0) { checksum, char -> checksum xor char.code }

    fun append(payload: String): String = "\$${payload}*%02X\r\n".format(calculate(payload))

    fun isValid(sentence: String): Boolean {
        val clean = sentence.trimEnd('\r', '\n')
        if (!clean.startsWith('$')) return false
        val star = clean.lastIndexOf('*')
        if (star <= 1 || star + 3 != clean.length) return false
        val expected = clean.substring(star + 1).toIntOrNull(16) ?: return false
        return calculate(clean.substring(1, star)) == expected
    }
}

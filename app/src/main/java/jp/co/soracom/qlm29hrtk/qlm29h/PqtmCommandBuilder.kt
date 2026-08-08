package jp.co.soracom.qlm29hrtk.qlm29h

import jp.co.soracom.qlm29hrtk.nmea.NmeaChecksum

object PqtmCommandBuilder {
    fun enableRtk(): ByteArray = NmeaChecksum.append("PQTMCFGRTK,W,1,1").toByteArray(Charsets.US_ASCII)
    fun version(): ByteArray = NmeaChecksum.append("PQTMVERNO").toByteArray(Charsets.US_ASCII)
}

package jp.co.soracom.qlm29hrtk.ntrip

/** NTRIP-06: editable defaults matching the SORACOM QLM29H getting-started guide. */
object NtripDefaults {
    const val HOST = "qrtksa1.quectel.com"
    const val PORT = 2101
    const val MOUNT_POINT = "AUTO"
}

data class NtripConfig(
    val host: String = NtripDefaults.HOST,
    val port: Int = NtripDefaults.PORT,
    val mountPoint: String = NtripDefaults.MOUNT_POINT,
    val username: String = "",
    val password: String = "",
    val tls: Boolean = false,
    val connectTimeoutMillis: Int = 10_000,
    val ggaIntervalMillis: Long = 1_000,
) {
    init {
        require(host.isNotBlank()) { "NTRIP host is required" }
        require(port in 1..65_535) { "NTRIP port is invalid" }
        require(mountPoint.isNotBlank()) { "NTRIP mount point is required" }
        require(connectTimeoutMillis > 0) { "NTRIP timeout must be positive" }
        require(ggaIntervalMillis > 0) { "GGA interval must be positive" }
    }
}

data class MountPoint(
    val name: String,
    val identifier: String?,
    val format: String?,
    val carrier: Int?,
    val latitude: Double?,
    val longitude: Double?,
    val requiresAuthentication: Boolean,
    val raw: String,
)

sealed interface NtripConnectResult {
    data object Success : NtripConnectResult
    data class Failure(val statusLine: String) : NtripConnectResult
}

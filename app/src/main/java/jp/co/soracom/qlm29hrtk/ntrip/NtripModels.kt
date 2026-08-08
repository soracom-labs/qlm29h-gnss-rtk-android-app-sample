package jp.co.soracom.qlm29hrtk.ntrip

data class NtripConfig(
    val host: String,
    val port: Int = 2101,
    val mountPoint: String,
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

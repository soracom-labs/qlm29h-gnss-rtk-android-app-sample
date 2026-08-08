package jp.co.soracom.qlm29hrtk.ntrip

import java.util.Base64

object NtripRequestBuilder {
    fun stream(config: NtripConfig): ByteArray {
        val mount = config.mountPoint.trim().trimStart('/')
        return buildRequest("/$mount", config.host, config.username, config.password)
    }

    fun sourceTable(host: String, username: String = "", password: String = ""): ByteArray =
        buildRequest("/", host, username, password)

    private fun buildRequest(path: String, host: String, username: String, password: String): ByteArray {
        val authorization = if (username.isNotEmpty() || password.isNotEmpty()) {
            val token = Base64.getEncoder().encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
            "Authorization: Basic $token\r\n"
        } else ""
        return buildString {
            append("GET $path HTTP/1.0\r\n")
            append("User-Agent: NTRIP AndroidClient/1.0\r\n")
            append("Host: $host\r\n")
            append("Accept: */*\r\n")
            append("Connection: close\r\n")
            append(authorization)
            append("\r\n")
        }.toByteArray(Charsets.US_ASCII)
    }
}

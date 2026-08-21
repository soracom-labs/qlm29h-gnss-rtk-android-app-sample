package jp.co.soracom.qlm29hrtk.ntrip

object NtripResponseParser {
    fun classify(statusLine: String): NtripConnectResult {
        val normalized = statusLine.trim()
        return if (
            normalized.equals("ICY 200 OK", ignoreCase = true) ||
            Regex("^HTTP/\\d(?:\\.\\d)?\\s+200(?:\\s|$)", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
        ) NtripConnectResult.Success else NtripConnectResult.Failure(normalized)
    }

    fun isSourceTableSuccess(statusLine: String): Boolean {
        val normalized = statusLine.trim()
        return normalized.equals("SOURCETABLE 200 OK", ignoreCase = true) ||
            classify(normalized) == NtripConnectResult.Success
    }

    fun statusCode(statusLine: String): Int? =
        Regex("^(?:(?:HTTP/\\d(?:\\.\\d)?|ICY)\\s+)?(\\d{3})(?:\\s|$)", RegexOption.IGNORE_CASE)
            .find(statusLine.trim())
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
}

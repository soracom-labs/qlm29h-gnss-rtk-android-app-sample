package jp.co.soracom.qlm29hrtk.soracom

enum class SoracomQualityPolicy { ALL_VALID, RTK_FLOAT_OR_BETTER, RTK_FIXED_ONLY }

data class SoracomSendPolicy(
    val sendNoFix: Boolean = false,
    val allowNtripDisconnected: Boolean = true,
    val qualityPolicy: SoracomQualityPolicy = SoracomQualityPolicy.ALL_VALID,
) {
    fun allows(quality: Int, ntripConnected: Boolean): Boolean {
        if (!ntripConnected && !allowNtripDisconnected) return false
        if (quality == 0) return sendNoFix && qualityPolicy == SoracomQualityPolicy.ALL_VALID
        return when (qualityPolicy) {
            SoracomQualityPolicy.ALL_VALID -> quality in setOf(1, 2, 4, 5, 6)
            SoracomQualityPolicy.RTK_FLOAT_OR_BETTER -> quality == 4 || quality == 5
            SoracomQualityPolicy.RTK_FIXED_ONLY -> quality == 4
        }
    }
}

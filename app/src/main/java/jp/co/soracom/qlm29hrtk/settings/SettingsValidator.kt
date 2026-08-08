package jp.co.soracom.qlm29hrtk.settings

data class ValidatedIntervals(val ntripPort: Int, val soracomIntervalSeconds: Int)

sealed interface SettingsValidationResult {
    data class Valid(val values: ValidatedIntervals) : SettingsValidationResult
    data class Invalid(val message: String) : SettingsValidationResult
}

/** One source of truth for numeric settings accepted by both UI and runtime. */
object SettingsValidator {
    fun validate(ntripPort: String, soracomIntervalSeconds: String): SettingsValidationResult {
        val port = ntripPort.toIntOrNull()
        if (port == null || port !in 1..65_535) {
            return SettingsValidationResult.Invalid("NTRIP port is invalid")
        }
        val interval = soracomIntervalSeconds.toIntOrNull()
        if (interval == null || interval !in 1..3_600) {
            return SettingsValidationResult.Invalid("SORACOM interval must be 1-3600 seconds")
        }
        return SettingsValidationResult.Valid(ValidatedIntervals(port, interval))
    }
}

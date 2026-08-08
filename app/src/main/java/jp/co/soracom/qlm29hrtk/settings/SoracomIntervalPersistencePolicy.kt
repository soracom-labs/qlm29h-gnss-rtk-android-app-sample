package jp.co.soracom.qlm29hrtk.settings

import jp.co.soracom.qlm29hrtk.soracom.SoracomSchedulePolicy

/** Keeps pre-confirmation settings from silently enabling high-frequency SORACOM traffic. */
object SoracomIntervalPersistencePolicy {
    const val VERSION = 1

    fun restore(savedSeconds: Int?, savedVersion: Int?): Int =
        savedSeconds?.takeIf {
            savedVersion == VERSION && SoracomSchedulePolicy.isAllowedInterval(it)
        } ?: SoracomSchedulePolicy.DEFAULT_INTERVAL_SECONDS
}

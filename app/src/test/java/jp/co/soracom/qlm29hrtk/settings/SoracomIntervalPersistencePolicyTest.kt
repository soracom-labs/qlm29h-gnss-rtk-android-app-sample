package jp.co.soracom.qlm29hrtk.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SoracomIntervalPersistencePolicyTest {
    @Test fun legacyIntervalFallsBackToDefaultUntilUserConfirmsAgain() {
        assertEquals(60, SoracomIntervalPersistencePolicy.restore(savedSeconds = 5, savedVersion = null))
    }

    @Test fun confirmedAllowedIntervalIsRestored() {
        assertEquals(
            5,
            SoracomIntervalPersistencePolicy.restore(
                savedSeconds = 5,
                savedVersion = SoracomIntervalPersistencePolicy.VERSION,
            ),
        )
    }

    @Test fun missingOrInvalidCurrentIntervalFallsBackToDefault() {
        val version = SoracomIntervalPersistencePolicy.VERSION
        assertEquals(60, SoracomIntervalPersistencePolicy.restore(savedSeconds = null, savedVersion = version))
        assertEquals(60, SoracomIntervalPersistencePolicy.restore(savedSeconds = 4, savedVersion = version))
    }
}

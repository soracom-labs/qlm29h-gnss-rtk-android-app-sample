package jp.co.soracom.qlm29hrtk.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {
    @Test fun acceptsDocumentedBoundaries() {
        assertEquals(
            ValidatedIntervals(1, 3_600),
            (SettingsValidator.validate("1", "3600") as SettingsValidationResult.Valid).values,
        )
        assertEquals(
            ValidatedIntervals(65_535, 1),
            (SettingsValidator.validate("65535", "1") as SettingsValidationResult.Valid).values,
        )
    }

    @Test fun rejectsInvalidPortBeforeInterval() {
        val result = SettingsValidator.validate("0", "0")
        assertTrue(result is SettingsValidationResult.Invalid)
        assertEquals("NTRIP port is invalid", (result as SettingsValidationResult.Invalid).message)
    }

    @Test fun rejectsIntervalOutsideDocumentedRange() {
        val result = SettingsValidator.validate("2101", "3601")
        assertEquals(
            "SORACOM interval must be 1-3600 seconds",
            (result as SettingsValidationResult.Invalid).message,
        )
    }
}

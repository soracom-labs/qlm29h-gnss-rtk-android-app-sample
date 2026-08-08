package jp.co.soracom.qlm29hrtk.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {
    @Test fun acceptsOnlyDocumentedIntervalChoices() {
        listOf(3, 5, 6, 10, 15, 30, 60).forEach { interval ->
            assertEquals(
                ValidatedIntervals(2_101, interval),
                (SettingsValidator.validate("2101", interval.toString()) as SettingsValidationResult.Valid).values,
            )
        }
        assertTrue(SettingsValidator.validate("1", "60") is SettingsValidationResult.Valid)
        assertTrue(SettingsValidator.validate("65535", "60") is SettingsValidationResult.Valid)
    }

    @Test fun rejectsInvalidPortBeforeInterval() {
        val result = SettingsValidator.validate("0", "0")
        assertTrue(result is SettingsValidationResult.Invalid)
        assertEquals("NTRIP port is invalid", (result as SettingsValidationResult.Invalid).message)
    }

    @Test fun rejectsIntervalsOutsideDocumentedChoices() {
        listOf("2", "4", "7", "61", "3600", "invalid").forEach { interval ->
            val result = SettingsValidator.validate("2101", interval)
            assertEquals(
                "SORACOM interval must be one of 3, 5, 6, 10, 15, 30, 60 seconds",
                (result as SettingsValidationResult.Invalid).message,
            )
        }
    }
}

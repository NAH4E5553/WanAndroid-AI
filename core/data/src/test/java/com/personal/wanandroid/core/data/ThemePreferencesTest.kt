package com.personal.wanandroid.core.data

import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferencesTest {
    @Test
    fun stableStorageValuesRoundTrip() {
        ThemePalettePreference.entries.forEach { palette ->
            assertEquals(palette, decodeThemePalette(palette.storageValue))
        }
        ThemeModePreference.entries.forEach { mode ->
            assertEquals(mode, decodeThemeMode(mode.storageValue))
        }
    }

    @Test
    fun missingOrUnknownValuesReturnSlateBlueFollowingSystem() {
        for (value in listOf(null, "", "future_value")) {
            assertEquals(ThemePalettePreference.SLATE_BLUE, decodeThemePalette(value))
            assertEquals(ThemeModePreference.FOLLOW_SYSTEM, decodeThemeMode(value))
        }
    }

    @Test
    fun ioReadFailureIsObservableAndUsesDefaults() = runTest {
        val state = flow<Preferences> { throw IOException("fixture") }
            .asThemePreferencesState()
            .first()

        assertTrue(state.readFailed)
        assertEquals(ThemePreferences(), state.preferences)
    }

    @Test
    fun successfulReadIsNotReportedAsFailure() = runTest {
        val state = flow<Preferences> {
            emit(androidx.datastore.preferences.core.emptyPreferences())
        }.asThemePreferencesState().first()

        assertFalse(state.readFailed)
        assertEquals(ThemePreferences(), state.preferences)
    }

    @Test(expected = CancellationException::class)
    fun cancellationIsNotConvertedToDefaultState() = runTest {
        flow<Preferences> { throw CancellationException("fixture") }
            .asThemePreferencesState()
            .first()
    }
}

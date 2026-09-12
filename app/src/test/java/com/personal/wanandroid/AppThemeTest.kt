package com.personal.wanandroid

import com.personal.wanandroid.core.data.model.ThemeModePreference
import com.personal.wanandroid.core.data.model.ThemePalettePreference
import com.personal.wanandroid.core.designsystem.theme.WanPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {
    @Test
    fun everyPreferencePaletteMapsToTheMatchingDesignPalette() {
        val expected = mapOf(
            ThemePalettePreference.INK_TEAL to WanPalette.INK_TEAL,
            ThemePalettePreference.SLATE_BLUE to WanPalette.SLATE_BLUE,
            ThemePalettePreference.WARM_AMBER to WanPalette.WARM_AMBER,
            ThemePalettePreference.BERRY_ROSE to WanPalette.BERRY_ROSE
        )

        expected.forEach { (preference, palette) ->
            assertEquals(palette, preference.toWanPalette())
        }
    }

    @Test
    fun displayModeOnlyFollowsSystemWhenRequested() {
        assertFalse(ThemeModePreference.FOLLOW_SYSTEM.resolveDark(systemDark = false))
        assertTrue(ThemeModePreference.FOLLOW_SYSTEM.resolveDark(systemDark = true))
        assertFalse(ThemeModePreference.LIGHT.resolveDark(systemDark = true))
        assertTrue(ThemeModePreference.DARK.resolveDark(systemDark = false))
    }
}

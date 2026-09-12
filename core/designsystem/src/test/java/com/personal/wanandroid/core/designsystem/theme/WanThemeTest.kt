package com.personal.wanandroid.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WanThemeTest {
    @Test
    fun everyPaletteUsesItsApprovedLightAndDarkPrimary() {
        val expected = mapOf(
            WanPalette.INK_TEAL to (Color(0xFF006B5B) to Color(0xFF82D5C2)),
            WanPalette.SLATE_BLUE to (Color(0xFF315F84) to Color(0xFF9CCAFA)),
            WanPalette.WARM_AMBER to (Color(0xFF795900) to Color(0xFFEFC15C)),
            WanPalette.BERRY_ROSE to (Color(0xFF8A3F62) to Color(0xFFFFAFCE))
        )

        expected.forEach { (palette, colors) ->
            assertEquals(colors.first, wanColorScheme(palette, dark = false).primary)
            assertEquals(colors.second, wanColorScheme(palette, dark = true).primary)
        }
    }

    @Test
    fun palettesShareNeutralReadingSurfacesWithoutLegacyPurple() {
        val legacyPrimary = Color(0xFF465CFF)
        listOf(false, true).forEach { dark ->
            val schemes = WanPalette.entries.map { wanColorScheme(it, dark) }
            assertEquals(1, schemes.map { it.background }.distinct().size)
            assertEquals(1, schemes.map { it.surface }.distinct().size)
            schemes.forEach { scheme ->
                assertNotEquals(legacyPrimary, scheme.primary)
                assertNotEquals(legacyPrimary, scheme.primaryContainer)
                assertNotEquals(Color.Unspecified, scheme.surfaceContainerLowest)
                assertNotEquals(Color.Unspecified, scheme.primaryFixed)
            }
        }
    }
}

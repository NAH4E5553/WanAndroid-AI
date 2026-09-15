package com.personal.wanandroid.feature.profile.view

import android.graphics.Bitmap
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.data.model.ThemeModePreference
import com.personal.wanandroid.core.data.model.ThemePalettePreference
import com.personal.wanandroid.core.data.model.ThemePreferences
import com.personal.wanandroid.core.designsystem.theme.WanPalette
import com.personal.wanandroid.core.designsystem.theme.WanTheme
import com.personal.wanandroid.feature.profile.state.ThemeSettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemeSettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun darkTwoHundredPercentFontKeepsVectorControlsUsable() {
        var preferences by mutableStateOf(
            ThemePreferences(
                palette = ThemePalettePreference.SLATE_BLUE,
                mode = ThemeModePreference.DARK
            )
        )
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                WanTheme(palette = preferences.palette.toPalette(), dark = true) {
                    ThemeSettingsScreen(
                        uiState = ThemeSettingsUiState(preferences = preferences),
                        onBack = {},
                        onPaletteSelected = { preferences = preferences.copy(palette = it) },
                        onModeSelected = { preferences = preferences.copy(mode = it) },
                        snackbarHostState = remember { SnackbarHostState() }
                    )
                }
            }
        }

        compose.onNodeWithContentDescription("返回").assertIsDisplayed()
        compose.onNodeWithTag("palette-slate_blue").assertIsSelected()
        compose.onNodeWithTag("palette-warm_amber").performScrollTo().performClick()
        compose.onNodeWithTag("palette-warm_amber").assertIsSelected()
        compose.runOnIdle {
            assertEquals(ThemePalettePreference.WARM_AMBER, preferences.palette)
        }
        capture("theme-settings-dark-200.png")
    }

    private fun capture(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.cacheDir.resolve(name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

private fun ThemePalettePreference.toPalette() = when (this) {
    ThemePalettePreference.INK_TEAL -> WanPalette.INK_TEAL
    ThemePalettePreference.SLATE_BLUE -> WanPalette.SLATE_BLUE
    ThemePalettePreference.WARM_AMBER -> WanPalette.WARM_AMBER
    ThemePalettePreference.BERRY_ROSE -> WanPalette.BERRY_ROSE
}

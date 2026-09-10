package com.personal.wanandroid.core.data

enum class ThemePalettePreference {
    INK_TEAL,
    SLATE_BLUE,
    WARM_AMBER,
    BERRY_ROSE
}

enum class ThemeModePreference { FOLLOW_SYSTEM, LIGHT, DARK }

data class ThemePreferences(
    val palette: ThemePalettePreference = ThemePalettePreference.SLATE_BLUE,
    val mode: ThemeModePreference = ThemeModePreference.FOLLOW_SYSTEM
)

data class ThemePreferencesState(
    val preferences: ThemePreferences = ThemePreferences(),
    val readFailed: Boolean = false
)

enum class ThemeUpdateResult { SUCCESS, FAILURE }

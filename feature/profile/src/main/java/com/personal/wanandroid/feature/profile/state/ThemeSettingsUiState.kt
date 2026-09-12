package com.personal.wanandroid.feature.profile.state

import com.personal.wanandroid.core.data.model.ThemePreferences

data class ThemeSettingsUiState(
    val preferences: ThemePreferences = ThemePreferences(),
    val readFailed: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailureEvent: Long = 0
)

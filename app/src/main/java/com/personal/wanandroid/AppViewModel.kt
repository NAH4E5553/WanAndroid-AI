package com.personal.wanandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.ThemeModePreference
import com.personal.wanandroid.core.data.ThemePalettePreference
import com.personal.wanandroid.core.data.ThemePreferencesRepository
import com.personal.wanandroid.core.data.ThemePreferencesState
import com.personal.wanandroid.core.designsystem.WanPalette
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AppViewModel @Inject constructor(repository: ThemePreferencesRepository) : ViewModel() {
    val themeState = repository.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemePreferencesState()
    )
}

internal fun ThemePalettePreference.toWanPalette(): WanPalette = when (this) {
    ThemePalettePreference.INK_TEAL -> WanPalette.INK_TEAL
    ThemePalettePreference.SLATE_BLUE -> WanPalette.SLATE_BLUE
    ThemePalettePreference.WARM_AMBER -> WanPalette.WARM_AMBER
    ThemePalettePreference.BERRY_ROSE -> WanPalette.BERRY_ROSE
}

internal fun ThemeModePreference.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeModePreference.FOLLOW_SYSTEM -> systemDark
    ThemeModePreference.LIGHT -> false
    ThemeModePreference.DARK -> true
}

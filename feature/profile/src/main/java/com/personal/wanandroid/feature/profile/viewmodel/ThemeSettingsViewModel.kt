package com.personal.wanandroid.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.model.ThemeModePreference
import com.personal.wanandroid.core.data.model.ThemePalettePreference
import com.personal.wanandroid.core.data.model.ThemePreferences
import com.personal.wanandroid.core.data.model.ThemeUpdateResult
import com.personal.wanandroid.core.data.repository.ThemePreferencesRepository
import com.personal.wanandroid.feature.profile.state.ThemeSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeSettingsViewModel @Inject constructor(
    private val repository: ThemePreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var persisted = ThemePreferences()
    private var requested = persisted
    private var updateJob: Job? = null
    private var updateGeneration = 0L
    private var failureGeneration = 0L

    init {
        viewModelScope.launch {
            repository.state.collect { state ->
                persisted = state.preferences
                val saving = _uiState.value.isSaving
                if (!saving) requested = state.preferences
                _uiState.update { current ->
                    current.copy(
                        preferences = if (saving) current.preferences else state.preferences,
                        readFailed = state.readFailed
                    )
                }
            }
        }
    }

    fun selectPalette(palette: ThemePalettePreference) {
        enqueue(requested.copy(palette = palette))
    }

    fun selectMode(mode: ThemeModePreference) {
        enqueue(requested.copy(mode = mode))
    }

    fun clearSaveFailure(event: Long) {
        _uiState.update { current ->
            if (current.saveFailureEvent == event) current.copy(saveFailureEvent = 0) else current
        }
    }

    private fun enqueue(next: ThemePreferences) {
        if (next == requested) return
        requested = next
        val generation = ++updateGeneration
        _uiState.update {
            it.copy(preferences = next, isSaving = true, saveFailureEvent = 0)
        }
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            when (repository.update(next)) {
                ThemeUpdateResult.SUCCESS -> {
                    if (generation != updateGeneration) return@launch
                    persisted = next
                    _uiState.update { it.copy(preferences = next, isSaving = false) }
                }

                ThemeUpdateResult.FAILURE -> {
                    if (generation != updateGeneration) return@launch
                    requested = persisted
                    _uiState.update {
                        it.copy(
                            preferences = persisted,
                            isSaving = false,
                            saveFailureEvent = ++failureGeneration
                        )
                    }
                }
            }
        }
    }
}

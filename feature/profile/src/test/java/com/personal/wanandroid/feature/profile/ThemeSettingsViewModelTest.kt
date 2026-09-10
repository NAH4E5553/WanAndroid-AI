package com.personal.wanandroid.feature.profile

import androidx.lifecycle.viewModelScope
import com.personal.wanandroid.core.data.ThemeModePreference
import com.personal.wanandroid.core.data.ThemePalettePreference
import com.personal.wanandroid.core.data.ThemePreferences
import com.personal.wanandroid.core.data.ThemePreferencesRepository
import com.personal.wanandroid.core.data.ThemePreferencesState
import com.personal.wanandroid.core.data.ThemeUpdateResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successfulSelectionBecomesCurrentPreference() = runTest(dispatcher) {
        val repository = ImmediateThemeRepository()
        val viewModel = ThemeSettingsViewModel(repository)
        runCurrent()

        viewModel.selectPalette(ThemePalettePreference.INK_TEAL)
        runCurrent()

        assertEquals(ThemePalettePreference.INK_TEAL, viewModel.uiState.value.preferences.palette)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(ThemePalettePreference.INK_TEAL, repository.state.value.preferences.palette)
    }

    @Test
    fun failedWriteRestoresLastPersistedPreferenceAndSignalsOnce() = runTest(dispatcher) {
        val initial = ThemePreferences(
            ThemePalettePreference.WARM_AMBER,
            ThemeModePreference.LIGHT
        )
        val repository = ImmediateThemeRepository(initial, ThemeUpdateResult.FAILURE)
        val viewModel = ThemeSettingsViewModel(repository)
        runCurrent()

        viewModel.selectMode(ThemeModePreference.DARK)
        assertEquals(ThemeModePreference.DARK, viewModel.uiState.value.preferences.mode)
        runCurrent()

        val failed = viewModel.uiState.value
        assertEquals(initial, failed.preferences)
        assertFalse(failed.isSaving)
        assertTrue(failed.saveFailureEvent > 0)

        viewModel.clearSaveFailure(failed.saveFailureEvent)
        assertEquals(0L, viewModel.uiState.value.saveFailureEvent)
    }

    @Test
    fun rapidSelectionIgnoresCancelledOlderWrite() = runTest(dispatcher) {
        val repository = ControllableThemeRepository()
        val viewModel = ThemeSettingsViewModel(repository)
        runCurrent()

        viewModel.selectPalette(ThemePalettePreference.INK_TEAL)
        runCurrent()
        val old = repository.takeRequest()

        viewModel.selectPalette(ThemePalettePreference.BERRY_ROSE)
        runCurrent()
        val latest = repository.takeRequest()
        old.complete(ThemeUpdateResult.FAILURE)
        latest.complete(ThemeUpdateResult.SUCCESS)
        runCurrent()

        assertEquals(ThemePalettePreference.BERRY_ROSE, viewModel.uiState.value.preferences.palette)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(0L, viewModel.uiState.value.saveFailureEvent)
        viewModel.viewModelScope.cancel()
    }
}

private class ImmediateThemeRepository(
    initial: ThemePreferences = ThemePreferences(),
    private val result: ThemeUpdateResult = ThemeUpdateResult.SUCCESS
) : ThemePreferencesRepository {
    private val mutableState = MutableStateFlow(ThemePreferencesState(initial))
    override val state = mutableState.asStateFlow()

    override suspend fun update(preferences: ThemePreferences): ThemeUpdateResult {
        if (result == ThemeUpdateResult.SUCCESS) {
            mutableState.value = ThemePreferencesState(preferences)
        }
        return result
    }
}

private class ControllableThemeRepository : ThemePreferencesRepository {
    private val mutableState = MutableStateFlow(ThemePreferencesState())
    private val requests = ArrayDeque<ThemeRequest>()
    override val state = mutableState.asStateFlow()

    override suspend fun update(preferences: ThemePreferences): ThemeUpdateResult =
        suspendCoroutine { continuation -> requests += ThemeRequest(preferences, continuation) }

    fun takeRequest(): ThemeRequest = requests.removeFirst()
}

private data class ThemeRequest(
    val preferences: ThemePreferences,
    val continuation: Continuation<ThemeUpdateResult>
) {
    fun complete(result: ThemeUpdateResult) {
        continuation.resume(result)
    }
}

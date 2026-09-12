package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.data.datasource.ThemePreferencesDataSource
import com.personal.wanandroid.core.data.model.ThemePreferences
import com.personal.wanandroid.core.data.model.ThemePreferencesState
import com.personal.wanandroid.core.data.model.ThemeUpdateResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val state: Flow<ThemePreferencesState>

    suspend fun update(preferences: ThemePreferences): ThemeUpdateResult
}

internal class DefaultThemePreferencesRepository @Inject constructor(
    private val dataSource: ThemePreferencesDataSource
) : ThemePreferencesRepository {
    override val state: Flow<ThemePreferencesState> = dataSource.state

    override suspend fun update(preferences: ThemePreferences): ThemeUpdateResult =
        dataSource.update(preferences)
}

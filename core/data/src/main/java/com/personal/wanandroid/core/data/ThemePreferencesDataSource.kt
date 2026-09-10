package com.personal.wanandroid.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val STORE_NAME = "theme_preferences"
private val Context.themePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = STORE_NAME
)

internal interface ThemePreferencesDataSource {
    val state: Flow<ThemePreferencesState>

    suspend fun update(preferences: ThemePreferences): ThemeUpdateResult
}

internal class PreferencesThemeDataSource @Inject constructor(
    @ApplicationContext context: Context
) : ThemePreferencesDataSource {
    private val dataStore = context.themePreferencesDataStore

    override val state: Flow<ThemePreferencesState> = dataStore.data.asThemePreferencesState()

    override suspend fun update(preferences: ThemePreferences): ThemeUpdateResult = try {
        dataStore.edit { values ->
            values[PaletteKey] = preferences.palette.storageValue
            values[ModeKey] = preferences.mode.storageValue
        }
        ThemeUpdateResult.SUCCESS
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ThemeUpdateResult.FAILURE
    }
}

internal fun Flow<Preferences>.asThemePreferencesState(): Flow<ThemePreferencesState> =
    map { ThemePreferencesState(it.toThemePreferences()) }
        .catch { error ->
            if (error is IOException) {
                emit(ThemePreferencesState(readFailed = true))
            } else {
                throw error
            }
        }

private val PaletteKey = stringPreferencesKey("theme_palette")
private val ModeKey = stringPreferencesKey("theme_mode")

internal fun Preferences.toThemePreferences() = ThemePreferences(
    palette = decodeThemePalette(this[PaletteKey]),
    mode = decodeThemeMode(this[ModeKey])
)

internal fun decodeThemePalette(value: String?): ThemePalettePreference = when (value) {
    "ink_teal" -> ThemePalettePreference.INK_TEAL
    "slate_blue" -> ThemePalettePreference.SLATE_BLUE
    "warm_amber" -> ThemePalettePreference.WARM_AMBER
    "berry_rose" -> ThemePalettePreference.BERRY_ROSE
    else -> ThemePalettePreference.SLATE_BLUE
}

internal fun decodeThemeMode(value: String?): ThemeModePreference = when (value) {
    "follow_system" -> ThemeModePreference.FOLLOW_SYSTEM
    "light" -> ThemeModePreference.LIGHT
    "dark" -> ThemeModePreference.DARK
    else -> ThemeModePreference.FOLLOW_SYSTEM
}

internal val ThemePalettePreference.storageValue: String
    get() = when (this) {
        ThemePalettePreference.INK_TEAL -> "ink_teal"
        ThemePalettePreference.SLATE_BLUE -> "slate_blue"
        ThemePalettePreference.WARM_AMBER -> "warm_amber"
        ThemePalettePreference.BERRY_ROSE -> "berry_rose"
    }

internal val ThemeModePreference.storageValue: String
    get() = when (this) {
        ThemeModePreference.FOLLOW_SYSTEM -> "follow_system"
        ThemeModePreference.LIGHT -> "light"
        ThemeModePreference.DARK -> "dark"
    }

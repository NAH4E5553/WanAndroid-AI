package com.personal.wanandroid.core.data.di

import com.personal.wanandroid.core.data.datasource.PreferencesSearchHistoryDataSource
import com.personal.wanandroid.core.data.datasource.PreferencesThemeDataSource
import com.personal.wanandroid.core.data.datasource.SearchHistoryDataSource
import com.personal.wanandroid.core.data.datasource.ThemePreferencesDataSource
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.data.repository.DefaultArticleRepository
import com.personal.wanandroid.core.data.repository.DefaultSearchSuggestionsRepository
import com.personal.wanandroid.core.data.repository.DefaultThemePreferencesRepository
import com.personal.wanandroid.core.data.repository.SearchSuggestionsRepository
import com.personal.wanandroid.core.data.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    internal abstract fun searchHistory(
        implementation: PreferencesSearchHistoryDataSource
    ): SearchHistoryDataSource

    @Binds
    @Singleton
    internal abstract fun searchSuggestions(
        implementation: DefaultSearchSuggestionsRepository
    ): SearchSuggestionsRepository

    @Binds
    @Singleton
    abstract fun bindArticleRepository(implementation: DefaultArticleRepository): ArticleRepository

    @Binds
    @Singleton
    internal abstract fun bindThemePreferencesDataSource(
        implementation: PreferencesThemeDataSource
    ): ThemePreferencesDataSource

    @Binds
    @Singleton
    internal abstract fun bindThemePreferencesRepository(
        implementation: DefaultThemePreferencesRepository
    ): ThemePreferencesRepository
}

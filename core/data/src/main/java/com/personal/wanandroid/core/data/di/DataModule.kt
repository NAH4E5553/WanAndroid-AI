package com.personal.wanandroid.core.data.di

import com.personal.wanandroid.core.data.datasource.EncryptedSessionStorage
import com.personal.wanandroid.core.data.datasource.PreferencesSearchHistoryDataSource
import com.personal.wanandroid.core.data.datasource.PreferencesThemeDataSource
import com.personal.wanandroid.core.data.datasource.SearchHistoryDataSource
import com.personal.wanandroid.core.data.datasource.ThemePreferencesDataSource
import com.personal.wanandroid.core.data.repository.ArticleRepository
import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.data.repository.CollectionRepository
import com.personal.wanandroid.core.data.repository.DefaultArticleRepository
import com.personal.wanandroid.core.data.repository.DefaultAuthRepository
import com.personal.wanandroid.core.data.repository.DefaultCollectionRepository
import com.personal.wanandroid.core.data.repository.DefaultReadingHistoryRepository
import com.personal.wanandroid.core.data.repository.DefaultSearchSuggestionsRepository
import com.personal.wanandroid.core.data.repository.DefaultThemePreferencesRepository
import com.personal.wanandroid.core.data.repository.ReadingHistoryRepository
import com.personal.wanandroid.core.data.repository.SearchSuggestionsRepository
import com.personal.wanandroid.core.data.repository.ThemePreferencesRepository
import com.personal.wanandroid.core.network.session.SessionStorage
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
    abstract fun readingHistory(
        implementation: DefaultReadingHistoryRepository
    ): ReadingHistoryRepository

    @Binds
    @Singleton
    abstract fun collections(implementation: DefaultCollectionRepository): CollectionRepository

    @Binds
    @Singleton
    internal abstract fun sessionStorage(implementation: EncryptedSessionStorage): SessionStorage

    @Binds
    @Singleton
    internal abstract fun authRepository(implementation: DefaultAuthRepository): AuthRepository

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

package com.personal.wanandroid.core.network.di

import com.personal.wanandroid.core.network.datasource.ArticleNetworkDataSource
import com.personal.wanandroid.core.network.datasource.AuthNetworkDataSource
import com.personal.wanandroid.core.network.datasource.CollectionNetworkDataSource
import com.personal.wanandroid.core.network.datasource.RetrofitArticleDataSource
import com.personal.wanandroid.core.network.datasource.RetrofitAuthDataSource
import com.personal.wanandroid.core.network.datasource.RetrofitCollectionDataSource
import com.personal.wanandroid.core.network.datasource.RetrofitSearchHotKeyDataSource
import com.personal.wanandroid.core.network.datasource.SearchHotKeyDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    abstract fun collections(
        implementation: RetrofitCollectionDataSource
    ): CollectionNetworkDataSource

    @Binds
    abstract fun auth(implementation: RetrofitAuthDataSource): AuthNetworkDataSource

    @Binds
    abstract fun searchHotKeys(
        implementation: RetrofitSearchHotKeyDataSource
    ): SearchHotKeyDataSource

    @Binds
    abstract fun articles(implementation: RetrofitArticleDataSource): ArticleNetworkDataSource
}

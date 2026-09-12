package com.personal.wanandroid.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }

    // Milestone 1 is public-read only. No credential persistence or body logging.
    @Provides
    @Singleton
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun service(client: OkHttpClient, json: Json): ArticleService = Retrofit.Builder()
        .baseUrl("https://wanandroid.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ArticleService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    abstract fun searchHotKeys(
        implementation: RetrofitSearchHotKeyDataSource
    ): SearchHotKeyDataSource

    @Binds
    abstract fun articles(implementation: RetrofitArticleDataSource): ArticleNetworkDataSource
}

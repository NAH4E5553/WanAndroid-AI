package com.personal.wanandroid.core.network.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.personal.wanandroid.core.network.interceptor.CollectionTraceInterceptor
import com.personal.wanandroid.core.network.interceptor.SessionInterceptor
import com.personal.wanandroid.core.network.service.ArticleService
import com.personal.wanandroid.core.network.service.AuthService
import com.personal.wanandroid.core.network.service.CollectionService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    // Fixed-origin session transport; no network body logging or WebView cookie sharing.
    @Provides
    @Singleton
    fun provideClient(
        sessionInterceptor: SessionInterceptor,
        @ApplicationContext context: Context
    ): OkHttpClient = client(sessionInterceptor).newBuilder().apply {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            addInterceptor(CollectionTraceInterceptor { Log.d("CollectionTrace", it) })
        }
    }.build()

    // Shared transport factory also used by fixed-response tests.
    fun client(sessionInterceptor: SessionInterceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(sessionInterceptor)
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl("https://wanandroid.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    fun service(retrofit: Retrofit): ArticleService = retrofit.create(ArticleService::class.java)

    @Provides
    fun collectionService(retrofit: Retrofit): CollectionService =
        retrofit.create(CollectionService::class.java)

    @Provides
    fun authService(retrofit: Retrofit): AuthService = retrofit.create(AuthService::class.java)
}

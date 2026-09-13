package com.personal.wanandroid

import com.personal.wanandroid.core.data.repository.AuthRepository
import com.personal.wanandroid.core.navigation.AuthStateReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NavigationAuthModule {
    @Provides
    fun authStateReader(repository: AuthRepository): AuthStateReader = object : AuthStateReader {
        override fun authenticatedAccountId() = repository.authenticatedAccountId()
        override fun knownAccountId() = repository.knownAccountId()
    }
}

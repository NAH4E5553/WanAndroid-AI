package com.personal.wanandroid.core.database.di

import android.content.Context
import androidx.room.Room
import com.personal.wanandroid.core.database.ReadingDatabase
import com.personal.wanandroid.core.database.dao.ReadingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun history(database: ReadingDatabase): ReadingDao = database.history()

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ReadingDatabase =
        Room.databaseBuilder(context, ReadingDatabase::class.java, "wanandroid-reading.db").build()
}

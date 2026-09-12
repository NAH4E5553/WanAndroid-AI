package com.personal.wanandroid.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.personal.wanandroid.core.database.dao.OfflineArticleDao
import com.personal.wanandroid.core.database.dao.ReadingDao
import com.personal.wanandroid.core.database.entity.OfflineArticleEntity
import com.personal.wanandroid.core.database.entity.ReadingHistoryEntity

@Database(
    entities = [ReadingHistoryEntity::class, OfflineArticleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ReadingDatabase : RoomDatabase() {
    abstract fun history(): ReadingDao
    abstract fun offline(): OfflineArticleDao
}

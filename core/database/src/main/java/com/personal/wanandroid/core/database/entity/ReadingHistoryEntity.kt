package com.personal.wanandroid.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "reading_history", indices = [Index("lastReadAt")])
data class ReadingHistoryEntity(
    @PrimaryKey val url: String,
    val articleId: Long?,
    val title: String,
    val lastReadAt: Long
)

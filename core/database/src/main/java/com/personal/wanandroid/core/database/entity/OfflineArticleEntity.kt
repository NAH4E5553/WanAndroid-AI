package com.personal.wanandroid.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// No cascading foreign key: deleting history must not delete offline content.
@Entity(tableName = "offline_articles", indices = [Index("lastAccessedAt")])
data class OfflineArticleEntity(
    @PrimaryKey val url: String,
    val title: String,
    val sanitizedHtml: String?,
    val status: String,
    val cachedAt: Long?,
    val lastAccessedAt: Long,
    val byteCount: Long
)

package com.personal.wanandroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.wanandroid.core.database.entity.OfflineArticleEntity
import com.personal.wanandroid.core.database.model.OfflineArticleSummary

@Dao
interface OfflineArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entry: OfflineArticleEntity)

    @Query(
        "SELECT url, title, status, cachedAt, byteCount FROM offline_articles ORDER BY lastAccessedAt DESC, url ASC LIMIT :limit OFFSET :offset"
    )
    suspend fun summaries(limit: Int, offset: Int): List<OfflineArticleSummary>

    @Query("SELECT * FROM offline_articles WHERE url = :url")
    suspend fun content(url: String): OfflineArticleEntity?

    @Query("DELETE FROM offline_articles WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM offline_articles")
    suspend fun clear()
}

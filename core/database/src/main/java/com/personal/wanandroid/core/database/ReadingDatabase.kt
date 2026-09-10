package com.personal.wanandroid.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "reading_history", indices = [Index("lastReadAt")])
data class ReadingHistoryEntity(
    @PrimaryKey val url: String,
    val articleId: Long?,
    val title: String,
    val lastReadAt: Long
)

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

data class OfflineArticleSummary(
    val url: String,
    val title: String,
    val status: String,
    val cachedAt: Long?,
    val byteCount: Long
)

@Dao
interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun record(entry: ReadingHistoryEntity)

    @Query(
        "SELECT * FROM reading_history ORDER BY lastReadAt DESC, url ASC LIMIT :limit OFFSET :offset"
    )
    suspend fun history(limit: Int, offset: Int): List<ReadingHistoryEntity>

    @Query("DELETE FROM reading_history WHERE url = :url")
    suspend fun deleteHistory(url: String)

    @Query("DELETE FROM reading_history")
    suspend fun clearHistory()
}

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

@Database(
    entities = [ReadingHistoryEntity::class, OfflineArticleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ReadingDatabase : RoomDatabase() {
    abstract fun history(): ReadingDao
    abstract fun offline(): OfflineArticleDao
}

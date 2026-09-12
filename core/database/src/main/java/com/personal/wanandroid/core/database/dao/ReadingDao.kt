package com.personal.wanandroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.personal.wanandroid.core.database.entity.ReadingHistoryEntity

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

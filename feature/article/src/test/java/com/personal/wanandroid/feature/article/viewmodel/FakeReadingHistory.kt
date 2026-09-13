package com.personal.wanandroid.feature.article.viewmodel

import com.personal.wanandroid.core.data.repository.ReadingHistoryRepository
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.flow.emptyFlow

internal class FakeReadingHistory : ReadingHistoryRepository {
    val records = mutableListOf<Triple<String, Long?, String>>()
    var fail = false
    override val changes = emptyFlow<Unit>()
    override suspend fun page(page: Int) =
        DataResult.Success(PageResult<ReadingHistory>(emptyList(), null))
    override suspend fun record(url: String, articleId: Long?, title: String): DataResult<Unit> {
        records.add(Triple(url, articleId, title))
        return if (fail) DataResult.Failure(DataError.STORAGE) else DataResult.Success(Unit)
    }
    override suspend fun delete(url: String) = DataResult.Success(Unit)
    override suspend fun clear() = DataResult.Success(Unit)
}

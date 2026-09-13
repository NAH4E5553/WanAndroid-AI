package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.database.dao.ReadingDao
import com.personal.wanandroid.core.database.entity.ReadingHistoryEntity
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.ReadingHistory
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface ReadingHistoryRepository {
    /** Emits on subscription and after history invalidation, including same-size updates. */
    val changes: Flow<Unit>
    suspend fun page(page: Int): DataResult<PageResult<ReadingHistory>>
    suspend fun record(url: String, articleId: Long?, title: String): DataResult<Unit>
    suspend fun delete(url: String): DataResult<Unit>
    suspend fun clear(): DataResult<Unit>
}

class DefaultReadingHistoryRepository internal constructor(
    private val dao: ReadingDao,
    private val now: () -> Long
) : ReadingHistoryRepository {
    @Inject constructor(
        dao: ReadingDao
    ) : this(dao, System::currentTimeMillis)

    private val writes = Mutex()

    override val changes = dao.changes().map { Unit }
    override suspend fun page(page: Int): DataResult<PageResult<ReadingHistory>> = storage {
        require(page >= 0 && page <= (Int.MAX_VALUE - PAGE_SIZE) / PAGE_SIZE)
        val rows = dao.history(PAGE_SIZE + 1, page * PAGE_SIZE)
        PageResult(
            rows.take(PAGE_SIZE).map {
                ReadingHistory(it.url, it.articleId, it.title, it.lastReadAt)
            },
            if (rows.size > PAGE_SIZE) page + 1 else null
        )
    }
    override suspend fun record(url: String, articleId: Long?, title: String): DataResult<Unit> {
        val key = canonicalUrl(url) ?: return DataResult.Failure(DataError.INVALID_RESPONSE)
        return writes.withLock {
            storage {
                dao.record(
                    ReadingHistoryEntity(
                        key,
                        articleId?.takeIf {
                            it >= 0
                        },
                        title.trim().take(200).ifBlank { key },
                        now()
                    )
                )
            }
        }
    }
    override suspend fun delete(url: String): DataResult<Unit> =
        writes.withLock { storage { dao.deleteHistory(url) } }
    override suspend fun clear(): DataResult<Unit> = writes.withLock {
        storage { dao.clearHistory() }
    }

    private suspend fun <T> storage(block: suspend () -> T): DataResult<T> =
        withContext(Dispatchers.IO) {
            try {
                DataResult.Success(block())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DataResult.Failure(DataError.STORAGE)
            }
        }
    companion object {
        private const val PAGE_SIZE = 20

        /** Ignore fragments/default port only; retain path case and query identity. */
        internal fun canonicalUrl(value: String): String? = try {
            val uri = URI(value)
            if (value.length > 8192 || !uri.scheme.equals("https", true) || uri.host == null ||
                uri.rawUserInfo != null || uri.port !in listOf(-1, 443)
            ) {
                null
            } else {
                "https://" + uri.host.lowercase(java.util.Locale.ROOT) +
                    uri.rawPath.orEmpty().ifEmpty { "/" } +
                    (uri.rawQuery?.let { "?$it" } ?: "")
            }
        } catch (_: Exception) {
            null
        }
    }
}

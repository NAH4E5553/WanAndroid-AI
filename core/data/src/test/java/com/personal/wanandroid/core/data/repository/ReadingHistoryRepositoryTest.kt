package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.database.dao.ReadingDao
import com.personal.wanandroid.core.database.entity.ReadingHistoryEntity
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReadingHistoryRepositoryTest {
    private class Dao : ReadingDao {
        val rows = mutableMapOf<String, ReadingHistoryEntity>()
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        var started = CompletableDeferred<Unit>()
        override fun changes() = emptyFlow<Int>()
        override suspend fun record(entry: ReadingHistoryEntity) {
            started.complete(Unit)
            gate?.await()
            if (fail) error("fixture storage error")
            rows[entry.url] = entry
        }
        override suspend fun history(limit: Int, offset: Int): List<ReadingHistoryEntity> {
            if (fail) error("fixture storage error")
            return rows.values.sortedWith(
                compareByDescending<ReadingHistoryEntity> {
                    it.lastReadAt
                }.thenBy { it.url }
            ).drop(offset).take(limit)
        }
        override suspend fun deleteHistory(url: String) {
            rows.remove(url)
        }
        override suspend fun clearHistory() {
            rows.clear()
        }
    }
    private val dao = Dao()
    private var time = 10L
    private val repo = DefaultReadingHistoryRepository(dao) { time++ }

    @Test fun revisitsNormalizeFragmentsAndPreserveQueryIdentity() = runTest {
        repo.record("https://READER.invalid:443/a#one", 1, "First")
        repo.record("https://reader.invalid/a#two", 1, "Latest")
        repo.record("https://reader.invalid/a?q=1", null, "Query")
        val page = (repo.page(0) as DataResult.Success).value
        assertEquals(listOf("Query", "Latest"), page.items.map { it.title })
        assertEquals("https://reader.invalid/a", page.items.last().url)
        assertNull(page.nextPage)
    }

    @Test fun pagesUseLookaheadAndStopAtExactBoundary() = runTest {
        repeat(21) { repo.record("https://reader.invalid/$it", it.toLong(), "$it") }
        assertEquals(20, (repo.page(0) as DataResult.Success).value.items.size)
        assertEquals(1, (repo.page(0) as DataResult.Success).value.nextPage)
        assertEquals(1, (repo.page(1) as DataResult.Success).value.items.size)
        repo.delete("https://reader.invalid/0")
        assertNull((repo.page(0) as DataResult.Success).value.nextPage)
        repo.clear()
        assertTrue((repo.page(0) as DataResult.Success).value.items.isEmpty())
    }

    @Test fun unsafeUrlsAreRejectedAndStorageFailureIsExplicit() = runTest {
        for (url in listOf(
            "http://reader.invalid",
            "file:///tmp/x",
            "https://user:password@reader.invalid",
            "https://reader.invalid:444/a"
        )) {
            assertEquals(
                DataResult.Failure(DataError.INVALID_RESPONSE),
                repo.record(url, null, "Bad")
            )
        }
        dao.fail = true
        assertEquals(
            DataResult.Failure(DataError.STORAGE),
            repo.record("https://reader.invalid", null, "Good")
        )
        assertEquals(DataResult.Failure(DataError.STORAGE), repo.page(0))
    }

    @Test fun cancellationIsRethrownAndClearCannotBeOvertakenByEarlierWrite() = runTest {
        dao.gate = CompletableDeferred()
        val record = async { repo.record("https://reader.invalid", 1, "Pending") }
        dao.started.await()
        val clear = async { repo.clear() }
        dao.gate!!.complete(Unit)
        record.await()
        clear.await()
        assertTrue(dao.rows.isEmpty())
        dao.gate = CompletableDeferred()
        dao.started = CompletableDeferred()
        val cancelled = async { repo.record("https://reader.invalid/cancel", null, "Cancelled") }
        dao.started.await()
        cancelled.cancel()
        try {
            cancelled.await()
            fail("Expected cancellation")
        } catch (
            _: CancellationException
        ) { }
        assertTrue(dao.rows.isEmpty())
    }
}

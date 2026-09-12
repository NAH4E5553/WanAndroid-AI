package com.personal.wanandroid.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SearchHistoryDataSourceTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun keepsLatestTwentyDistinctQueriesAcrossReopeningAndClearPersists() = runTest {
        val file = temporary.newFolder().resolve("history.preferences_pb")
        fun open(job: Job) = PreferencesSearchHistoryDataSource(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
        )
        var job = SupervisorJob()
        var source = open(job)
        try {
            for (index in 1..25) assertTrue(source.record("query $index"))
            assertEquals((25 downTo 6).map { "query $it" }, source.history.first().items)
            assertTrue(source.record("  query 10  "))
            assertTrue(source.record("   "))
            val expected =
                listOf("query 10") + (25 downTo 6).filter { it != 10 }.map { "query $it" }
            assertEquals(expected, source.history.first().items)
            job.cancelAndJoin()
            job = SupervisorJob()
            source = open(job)
            assertEquals(expected, source.history.first().items)
            assertTrue(source.clear())
            job.cancelAndJoin()
            job = SupervisorJob()
            source = open(job)
            assertTrue(source.history.first().items.isEmpty())
            assertTrue(source.record("after clear"))
            assertEquals(listOf("after clear"), source.history.first().items)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test fun ioFailureIsReportedAndCancellationPropagates() = runTest {
        val failing = PreferencesSearchHistoryDataSource(BrokenStore(IOException("fixture")))
        assertTrue(failing.history.first().readFailed)
        assertFalse(failing.record("query"))
        assertFalse(failing.clear())
        val cancelled = PreferencesSearchHistoryDataSource(BrokenStore(CancellationException()))
        for (operation in listOf<suspend () -> Any>(
            { cancelled.history.first() },
            { cancelled.record("query") },
            { cancelled.clear() }
        )) {
            var propagated = false
            try {
                operation()
            } catch (_: CancellationException) {
                propagated = true
            }
            assertTrue(propagated)
        }
    }
}
private class BrokenStore(private val failure: Exception) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw failure }
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        throw failure
}

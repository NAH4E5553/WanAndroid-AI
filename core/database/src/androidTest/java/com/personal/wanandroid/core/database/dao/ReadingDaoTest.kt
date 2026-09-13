package com.personal.wanandroid.core.database.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.personal.wanandroid.core.database.ReadingDatabase
import com.personal.wanandroid.core.database.entity.OfflineArticleEntity
import com.personal.wanandroid.core.database.entity.ReadingHistoryEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReadingDaoTest {
    private inline fun ReadingDatabase.withDatabase(block: (ReadingDatabase) -> Unit) {
        try {
            block(this)
        } finally {
            close()
        }
    }

    @Test fun upsertPaginationAndIndependentDeletion() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Room.inMemoryDatabaseBuilder(
            context,
            ReadingDatabase::class.java
        ).build().withDatabase { db ->
            val history = db.history()
            repeat(23) {
                history.record(
                    ReadingHistoryEntity(
                        "https://fixture.invalid/$it",
                        it.toLong(),
                        "$it",
                        it.toLong()
                    )
                )
            }
            history.record(ReadingHistoryEntity("https://fixture.invalid/0", 0, "Revisited", 100))
            assertEquals(20, history.history(20, 0).size)
            assertEquals(3, history.history(20, 20).size)
            assertEquals("Revisited", history.history(1, 0).single().title)
            val url = "https://fixture.invalid/0"
            db.offline().save(
                OfflineArticleEntity(url, "Cached", "<p>fixture</p>", "READY", 1, 1, 14)
            )
            history.deleteHistory(url)
            assertNotNull(db.offline().content(url))
            history.clearHistory()
            assertTrue(history.history(20, 0).isEmpty())
            assertNotNull(db.offline().content(url))
            history.record(ReadingHistoryEntity(url, 0, "New reading", 101))
            db.offline().clear()
            assertEquals(1, history.history(20, 0).size)
        }
    }

    @Test fun revisitingSameRowNotifiesAnActiveHistoryList() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Room.inMemoryDatabaseBuilder(
            context,
            ReadingDatabase::class.java
        ).build().withDatabase { db ->
            db.history().record(ReadingHistoryEntity("https://fixture.invalid", 1, "First", 1))
            val signals = Channel<Int>(Channel.UNLIMITED)
            val observer = launch { db.history().changes().collect { signals.send(it) } }
            try {
                assertEquals(1, withTimeout(5000) { signals.receive() })
                db.history().record(ReadingHistoryEntity("https://fixture.invalid", 1, "Again", 2))
                assertEquals(1, withTimeout(5000) { signals.receive() })
            } finally {
                observer.cancel()
                signals.close()
            }
        }
    }

    @Test fun storedHistorySurvivesDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-test-${java.util.UUID.randomUUID()}.db"
        try {
            Room.databaseBuilder(
                context,
                ReadingDatabase::class.java,
                name
            ).build().withDatabase { db ->
                db.history().record(
                    ReadingHistoryEntity("https://fixture.invalid", null, "Persisted", 123)
                )
            }
            Room.databaseBuilder(
                context,
                ReadingDatabase::class.java,
                name
            ).build().withDatabase { db ->
                assertEquals("Persisted", db.history().history(20, 0).single().title)
                assertTrue(db.offline().summaries(20, 0).isEmpty())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}

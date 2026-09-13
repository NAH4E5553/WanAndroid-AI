package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.network.datasource.CollectionNetworkDataSource
import com.personal.wanandroid.core.network.dto.CollectionDto
import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStorage
import com.personal.wanandroid.core.network.session.SessionStore
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import okhttp3.Cookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionRepositoryTest {
    private val store = SessionStore(object : SessionStorage {
        override fun read(): String? = null
        override fun write(payload: String?) = Unit
    }) { 1_000 }
    private val internal = CollectionTarget(42, 900)
    private val external = CollectionTarget(recordId = 901)
    private val dto = CollectionDto(900, 42, "Fixture", "https://example.invalid/article")
    private val source = Fake()
    private val repository = DefaultCollectionRepository(source, store)
    private fun signIn(id: Long = 7): Long {
        val request = store.beginLogin()
        val cookie = Cookie.Builder().name("session").value("fixture")
            .hostOnlyDomain("wanandroid.com").expiresAt(100_000).build()
        assertTrue(store.commitLogin(request, UserDto(id, "fixture"), listOf(cookie)))
        return request.generation
    }
    private inner class Fake : CollectionNetworkDataSource {
        var data = listOf(dto)
        var over = true
        var pageCalls = 0
        var lastReadThread: Thread? = null
        var writes = mutableListOf<String>()
        val writeStarted = CompletableDeferred<Unit>()
        var listStarted: CompletableDeferred<Unit>? = null
        var writeGate: CompletableDeferred<Unit>? = null
        var listGate: CompletableDeferred<Unit>? = null
        var nonCooperative = false
        var writeError: Exception? = null
        var readError: Exception? = null
        var businessCode = 0
        var applied = true
        override suspend fun list(
            page: Int,
            session: SessionRequest
        ): WanResponse<WanPageDto<CollectionDto>> {
            pageCalls++
            lastReadThread = Thread.currentThread()
            val snapshot = data
            listStarted?.complete(Unit)
            if (nonCooperative) {
                withContext(NonCancellable) {
                    listGate?.await()
                }
            } else {
                listGate?.await()
            }
            readError?.let { throw it }
            return WanResponse(0, data = WanPageDto(snapshot, page + 1, over, snapshot.size))
        }
        private suspend fun write(name: String, collected: Boolean): WanResponse<JsonElement> {
            writes.add(name)
            writeStarted.complete(Unit)
            writeGate?.await()
            if (applied && businessCode == 0) data = if (collected) listOf(dto) else emptyList()
            writeError?.let { throw it }
            return WanResponse(businessCode)
        }
        override suspend fun collect(articleId: Long, session: SessionRequest) =
            write("add:$articleId", true)
        override suspend fun uncollectArticle(articleId: Long, session: SessionRequest) =
            write("article:$articleId", false)
        override suspend fun uncollectRecord(
            recordId: Long,
            articleId: Long,
            session: SessionRequest
        ) = write("record:$recordId:$articleId", false)
    }

    @Test fun collectionAndArticleIdsRemainDistinctAndNullWriteDataSucceeds() = runTest {
        val generation = signIn()
        val page = repository.page(generation, 0) as DataResult.Success
        assertEquals(internal, page.value.items.single().target)
        assertEquals(42L, page.value.items.single().article.id)
        assertEquals(DataResult.Success(Unit), repository.setCollected(generation, internal, false))
        assertEquals(listOf("record:900:42"), source.writes)
        assertEquals(false, repository.current().status(CollectionTarget(42)).collected)
    }

    @Test fun externalRecordUsesMinusOneAndCannotBeReaddedAsInternalArticle() = runTest {
        val generation = signIn()
        source.data = listOf(dto.copy(id = 901, originId = -1))
        repository.page(generation, 0)
        repository.setCollected(generation, external, false)
        assertEquals(listOf("record:901:-1"), source.writes)
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            repository.setCollected(generation, external, true)
        )
        assertEquals(1, source.writes.size)
    }

    @Test fun readerUsesOriginEndpointAndAddsWithOriginalId() = runTest {
        val generation = signIn()
        val target = CollectionTarget(42)
        repository.reconcile(generation, target)
        repository.setCollected(generation, target, false)
        repository.setCollected(generation, target, true)
        assertEquals(true, repository.current().status(target).collected)
        repository.setCollected(generation, target, false)
        assertEquals(listOf("article:42", "add:42", "article:42"), source.writes)
        assertEquals(false, repository.current().status(target).collected)
    }

    @Test fun duplicateClicksAreNotQueued() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.writeGate = CompletableDeferred()
        val first =
            async(start = CoroutineStart.UNDISPATCHED) {
                repository.setCollected(generation, internal, false)
            }
        source.writeStarted.await()
        repository.setCollected(generation, internal, false)
        assertTrue(repository.current().status(internal).busy)
        assertEquals(1, source.writes.size)
        source.writeGate!!.complete(Unit)
        first.await()
        assertFalse(repository.current().status(internal).busy)
    }

    @Test fun failurePreservesKnownStateAndDoesNotAdvanceRevisionUntilWriteEnds() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.businessCode = -1
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            repository.setCollected(generation, internal, false)
        )
        assertEquals(true, repository.current().status(internal).collected)
        assertEquals(1L, repository.current().revision)
    }

    @Test fun timeoutAfterCommitReconcilesWithoutASecondPost() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.writeError = IOException()
        assertEquals(DataResult.Success(Unit), repository.setCollected(generation, internal, false))
        assertEquals(false, repository.current().status(internal).collected)
        assertEquals(1, source.writes.size)
        assertEquals(2, source.pageCalls)
    }

    @Test fun failedReconciliationLeavesUnknownAndNextClickOnlyReads() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.writeError = IOException()
        source.readError = IOException()
        repository.setCollected(generation, internal, false)
        assertNull(repository.current().status(internal).collected)
        source.readError = null
        repository.setCollected(generation, internal, false)
        assertEquals(false, repository.current().status(internal).collected)
        assertEquals(1, source.writes.size)
    }

    @Test fun cancelledWriteLeavesUnknownAndUnlocks() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.writeGate = CompletableDeferred()
        val task =
            async(start = CoroutineStart.UNDISPATCHED) {
                repository.setCollected(generation, internal, false)
            }
        source.writeStarted.await()
        task.cancel()
        task.join()
        assertNull(repository.current().status(internal).collected)
        assertFalse(repository.current().status(internal).busy)
        assertEquals(1L, repository.current().revision)
    }

    @Test fun oldWriteCannotPublishOrAttachToNewAccount() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.writeGate = CompletableDeferred()
        val task =
            async(start = CoroutineStart.UNDISPATCHED) {
                repository.setCollected(generation, internal, false)
            }
        source.writeStarted.await()
        val next = signIn(8)
        source.writeGate!!.complete(Unit)
        assertEquals(DataResult.Failure(DataError.SESSION_CHANGED), task.await())
        assertEquals(next, repository.current().generation)
        assertTrue(repository.current().statuses.isEmpty())
        repository.setCollected(generation, internal, true)
        assertEquals(1, source.writes.size)
    }

    @Test fun staleReadCannotRestoreOldCollectionStateAfterWrite() = runTest {
        val generation = signIn()
        repository.page(generation, 0)
        source.listGate = CompletableDeferred()
        source.listStarted = CompletableDeferred()
        val page = async(start = CoroutineStart.UNDISPATCHED) { repository.page(generation, 0) }
        source.listStarted!!.await()
        repository.setCollected(generation, internal, false)
        source.listGate!!.complete(Unit)
        assertEquals(DataResult.Failure(DataError.SESSION_CHANGED), page.await())
        assertEquals(false, repository.current().status(internal).collected)
    }

    @Test fun cancelledNonCooperativeReadDoesNotSeedCache() = runTest {
        val generation = signIn()
        source.listGate = CompletableDeferred()
        source.listStarted = CompletableDeferred()
        source.nonCooperative = true
        val page = async(start = CoroutineStart.UNDISPATCHED) { repository.page(generation, 0) }
        source.listStarted!!.await()
        page.cancel()
        source.listGate!!.complete(Unit)
        page.join()
        assertTrue(repository.current().statuses.isEmpty())
    }

    @Test fun boundedScanNeverTreatsPartialAbsenceAsUncollected() = runTest {
        val generation = signIn()
        source.data = emptyList()
        source.over = false
        assertEquals(
            DataResult.Failure(DataError.NETWORK),
            repository.reconcile(generation, internal)
        )
        assertNull(repository.current().status(internal).collected)
        assertEquals(50, source.pageCalls)
        assertTrue(source.writes.isEmpty())
    }

    @Test fun guestAndOldGenerationNeverSendRequests() = runTest {
        val generation = signIn()
        store.detach()
        assertNull(repository.current().generation)
        assertTrue(repository.current().statuses.isEmpty())
        repository.page(generation, 0)
        repository.reconcile(generation, internal)
        repository.setCollected(generation, internal, false)
        assertEquals(0, source.pageCalls)
        assertTrue(source.writes.isEmpty())
    }

    @Test fun collectionRequestsLeaveTheCallerThread() = runTest {
        val generation = signIn()
        val caller = Thread.currentThread()
        repository.page(generation, 0)
        assertNotSame(caller, source.lastReadThread)
    }

    @Test fun oldReadCannotSeedANewAccountsSnapshot() = runTest {
        val generation = signIn()
        source.listGate = CompletableDeferred()
        source.listStarted = CompletableDeferred()
        val page = async { repository.page(generation, 0) }
        source.listStarted!!.await()
        val next = signIn(8)
        source.listGate!!.complete(Unit)
        assertEquals(DataResult.Failure(DataError.SESSION_CHANGED), page.await())
        assertEquals(next, repository.current().generation)
        assertTrue(repository.current().statuses.isEmpty())
    }
}

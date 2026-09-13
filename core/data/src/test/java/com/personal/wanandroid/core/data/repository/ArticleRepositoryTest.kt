package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.network.datasource.ArticleNetworkDataSource
import com.personal.wanandroid.core.network.dto.ArticleDto
import com.personal.wanandroid.core.network.dto.TopicDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleRepositoryTest {
    @Test fun ordinaryArticleFeedsUseTheSameOfficialLinkNormalization() = runTest {
        val source = FakeSource().apply {
            page =
                WanResponse(
                    0,
                    data = WanPageDto(listOf(article(7).copy(link = "blog/show/123")), 1, true, 1)
                )
        }
        val result = articleRepository(source).articles(0) as DataResult.Success
        assertEquals("https://wanandroid.com/blog/show/123", result.value.items.single().url)
    }

    @Test fun everyArticleFeedPreservesCollectAndItsRequestSession() = runTest {
        val source = FakeSource()
        source.page =
            WanResponse(0, data = WanPageDto(listOf(article(7).copy(collect = true)), 1, true, 1))
        val fixture = ArticleCollectionFixture().apply { signIn() }
        val repository = DefaultArticleRepository(source, fixture.collections)
        val results = listOf(
            (repository.articles(0) as DataResult.Success).value.items,
            (repository.articles(0, 9) as DataResult.Success).value.items,
            (repository.questionPage(1) as DataResult.Success).value.items,
            (repository.questions() as DataResult.Success).value,
            (repository.search(0, "fixture") as DataResult.Success).value.items
        )
        results.forEach { rows ->
            assertTrue(rows.single().collected)
            assertEquals(
                fixture.sessions.authenticatedVersionKey(),
                rows.single().collectionSession
            )
            assertEquals(true, fixture.collections.current().status(CollectionTarget(7)).collected)
            assertEquals("https://example.org/7", rows.single().url)
        }
    }

    @Test fun accountChangeDuringListRequestRejectsOldResponse() = runTest {
        val source = FakeSource()
        val fixture = ArticleCollectionFixture().apply { signIn() }
        source.beforeResponse = { fixture.signIn(8) }
        val repository = DefaultArticleRepository(source, fixture.collections)
        assertEquals(DataResult.Failure(DataError.SESSION_CHANGED), repository.articles(0))
        assertTrue(fixture.collections.current().statuses.isEmpty())
    }

    @Test fun allArticleFeedsCaptureWriteVersionBeforeSendingTheRequest() = runTest {
        for (feed in 0..4) {
            val fixture = ArticleCollectionFixture().apply { signIn() }
            val source = FakeSource().apply {
                page = WanResponse(0, data = WanPageDto(listOf(article(7)), 1, true, 1))
            }
            val repo = DefaultArticleRepository(source, fixture.collections)
            repo.articles(0)
            val gate = CompletableDeferred<Unit>()
            source.beforeResponse = { gate.await() }
            val old = async(start = CoroutineStart.UNDISPATCHED) {
                when (feed) {
                    0 -> (repo.articles(0) as DataResult.Success).value.items
                    1 -> (repo.articles(0, 9) as DataResult.Success).value.items
                    2 -> (repo.questionPage(1) as DataResult.Success).value.items
                    3 -> (repo.questions() as DataResult.Success).value
                    else -> (repo.search(0, "fixture") as DataResult.Success).value.items
                }
            }
            fixture.collections.setCollected(
                fixture.collections.current().generation!!,
                CollectionTarget(7),
                true
            )
            gate.complete(Unit)
            assertTrue(old.await().single().collected)
            assertEquals(true, fixture.collections.current().status(CollectionTarget(7)).collected)
            source.beforeResponse = {}
            val fresh = repo.articles(0) as DataResult.Success
            assertEquals(false, fresh.value.items.single().collected)
            assertEquals(false, fixture.collections.current().status(CollectionTarget(7)).collected)
        }
    }

    @Test fun searchPreservesKeywordAndUsesRequestCursorAndOverFlag() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(0, data = WanPageDto(listOf(article(7)), 99, false, 1))
        val result = articleRepository(fake).search(0, "Kotlin + Flow") as DataResult.Success
        assertEquals("Kotlin + Flow", fake.searchKeyword)
        assertEquals(0, fake.requestPage)
        assertEquals(1, result.value.nextPage)
        assertEquals(7L, result.value.items.single().id)
        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 99, true, 0))
        val last = articleRepository(fake).search(1, "Kotlin + Flow") as DataResult.Success
        assertNull(last.value.nextPage)
    }

    @Test fun searchBusinessFailureAndMissingBodyAreNotEmptyResults() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(-1)
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            articleRepository(fake).search(0, "fixture")
        )
        fake.page = WanResponse(0)
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            articleRepository(fake).search(0, "fixture")
        )
    }

    @Test
    fun nextPageUsesRequestCursorNotResponsePageOrListSize() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 1, false, 10))
        val result = articleRepository(fake).articles(0) as DataResult.Success
        assertEquals(1, result.value.nextPage)
        assertEquals(0, fake.requestPage)
    }

    @Test
    fun terminalPageDoesNotAdvance() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 3, true, 0))
        val result = articleRepository(fake).articles(2) as DataResult.Success
        assertNull(result.value.nextPage)
    }

    @Test
    fun flattenRetainsParentAndDistinctSameNameChildren() = runTest {
        val result = articleRepository(FakeSource()).topics() as DataResult.Success
        assertEquals(listOf(10L, 11L, 12L), result.value.map { it.id })
        assertNull(result.value.first().parentId)
        assertEquals(10L, result.value[1].parentId)
    }

    @Test
    fun questionsAreLimitedToFive() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(
            0,
            data = WanPageDto((1L..8L).map { article(it) }, 1, false, 8)
        )
        val result = articleRepository(fake).questions() as DataResult.Success
        assertEquals(5, result.value.size)
        assertEquals(1, fake.questionRequestPage)
    }

    @Test
    fun questionPageUsesOneBasedRequestCursorAndOverFlag() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(
            0,
            data = WanPageDto((1L..8L).map { article(it) }, 1, false, 8)
        )

        val result = articleRepository(fake).questionPage(1) as DataResult.Success

        assertEquals(8, result.value.items.size)
        assertEquals(2, result.value.nextPage)
        assertEquals(1, fake.questionRequestPage)

        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 2, true, 8))
        val terminal = articleRepository(fake).questionPage(2) as DataResult.Success
        assertNull(terminal.value.nextPage)
    }

    @Test
    fun articleMetadataKeepsAuthorAndSharerSeparate() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(
            0,
            data = WanPageDto(
                datas = listOf(
                    ArticleDto(
                        id = 7,
                        title = "Example",
                        link = "https://example.org/7",
                        author = "",
                        shareUser = "Sharer",
                        superChapterName = "Parent",
                        chapterName = "Child",
                        niceDate = "1天前"
                    )
                ),
                curPage = 1,
                over = true,
                total = 1
            )
        )

        val result = articleRepository(fake).articles(0) as DataResult.Success
        val article = result.value.items.single()

        assertEquals("", article.author)
        assertEquals("Sharer", article.shareUser)
        assertEquals("Parent", article.superChapterName)
        assertEquals("Child", article.chapter)
        assertEquals("1天前", article.publishedAt)
    }

    @Test
    fun errorAndMissingDataAreNotSuccessfulEmptyLists() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(-1001)
        assertEquals(
            DataResult.Failure(DataError.SESSION_EXPIRED),
            articleRepository(fake).articles(0)
        )
        fake.page = WanResponse(-1)
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            articleRepository(fake).articles(0)
        )
        fake.page = WanResponse(0)
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            articleRepository(fake).articles(0)
        )
    }

    @Test
    fun ioFailuresUseNetworkError() = runTest {
        val fake = FakeSource()
        fake.failure = IOException("synthetic network failure")
        assertEquals(
            DataResult.Failure(DataError.NETWORK),
            articleRepository(fake).articles(0)
        )
    }

    @Test
    fun malformedResponseUsesStableErrorWithoutExceptionDetails() = runTest {
        val fake = FakeSource()
        fake.failure = IllegalArgumentException("synthetic malformed payload")
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            articleRepository(fake).articles(0)
        )
    }

    @Test
    fun cancellationIsNotConvertedToUiFailure() = runTest {
        val fake = FakeSource()
        fake.failure = CancellationException("superseded")
        var cancelled = false
        try {
            articleRepository(fake).articles(0)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}

private fun articleRepository(source: ArticleNetworkDataSource) =
    DefaultArticleRepository(source, ArticleCollectionFixture().collections)

private fun article(id: Long) = ArticleDto(id, "Example", "https://example.org/$id")

private class FakeSource : ArticleNetworkDataSource {
    var page: WanResponse<WanPageDto<ArticleDto>> = WanResponse(
        0,
        data = WanPageDto(listOf(article(1)), 1, true, 1)
    )
    var searchKeyword: String? = null
    var requestPage: Int? = null
    var questionRequestPage: Int? = null
    var failure: Exception? = null
    var beforeResponse: suspend () -> Unit = {}
    override suspend fun articles(
        page: Int,
        categoryId: Long?
    ): WanResponse<WanPageDto<ArticleDto>> {
        failure?.let { throw it }
        requestPage = page
        beforeResponse()
        return this.page
    }
    override suspend fun questions(page: Int): WanResponse<WanPageDto<ArticleDto>> {
        failure?.let { throw it }
        questionRequestPage = page
        beforeResponse()
        return this.page
    }
    override suspend fun search(page: Int, keyword: String): WanResponse<WanPageDto<ArticleDto>> {
        searchKeyword = keyword
        return articles(page, null)
    }
    override suspend fun topics() = WanResponse(
        0,
        data = listOf(TopicDto(10, "Parent", listOf(TopicDto(11, "Same"), TopicDto(12, "Same"))))
    )
}

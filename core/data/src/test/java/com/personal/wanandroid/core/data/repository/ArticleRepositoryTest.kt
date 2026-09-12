package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.network.datasource.ArticleNetworkDataSource
import com.personal.wanandroid.core.network.dto.ArticleDto
import com.personal.wanandroid.core.network.dto.TopicDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleRepositoryTest {
    @Test fun searchPreservesKeywordAndUsesRequestCursorAndOverFlag() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(0, data = WanPageDto(listOf(article(7)), 99, false, 1))
        val result = DefaultArticleRepository(fake).search(0, "Kotlin + Flow") as DataResult.Success
        assertEquals("Kotlin + Flow", fake.searchKeyword)
        assertEquals(0, fake.requestPage)
        assertEquals(1, result.value.nextPage)
        assertEquals(7L, result.value.items.single().id)
        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 99, true, 0))
        val last = DefaultArticleRepository(fake).search(1, "Kotlin + Flow") as DataResult.Success
        assertNull(last.value.nextPage)
    }

    @Test fun searchBusinessFailureAndMissingBodyAreNotEmptyResults() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(-1)
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            DefaultArticleRepository(fake).search(0, "fixture")
        )
        fake.page = WanResponse(0)
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            DefaultArticleRepository(fake).search(0, "fixture")
        )
    }

    @Test
    fun nextPageUsesRequestCursorNotResponsePageOrListSize() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 1, false, 10))
        val result = DefaultArticleRepository(fake).articles(0) as DataResult.Success
        assertEquals(1, result.value.nextPage)
        assertEquals(0, fake.requestPage)
    }

    @Test
    fun terminalPageDoesNotAdvance() = runTest {
        val fake = FakeSource()
        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 3, true, 0))
        val result = DefaultArticleRepository(fake).articles(2) as DataResult.Success
        assertNull(result.value.nextPage)
    }

    @Test
    fun flattenRetainsParentAndDistinctSameNameChildren() = runTest {
        val result = DefaultArticleRepository(FakeSource()).topics() as DataResult.Success
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
        val result = DefaultArticleRepository(fake).questions() as DataResult.Success
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

        val result = DefaultArticleRepository(fake).questionPage(1) as DataResult.Success

        assertEquals(8, result.value.items.size)
        assertEquals(2, result.value.nextPage)
        assertEquals(1, fake.questionRequestPage)

        fake.page = WanResponse(0, data = WanPageDto(emptyList(), 2, true, 8))
        val terminal = DefaultArticleRepository(fake).questionPage(2) as DataResult.Success
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

        val result = DefaultArticleRepository(fake).articles(0) as DataResult.Success
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
            DefaultArticleRepository(fake).articles(0)
        )
        fake.page = WanResponse(-1)
        assertEquals(
            DataResult.Failure(DataError.SERVICE),
            DefaultArticleRepository(fake).articles(0)
        )
        fake.page = WanResponse(0)
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            DefaultArticleRepository(fake).articles(0)
        )
    }

    @Test
    fun ioFailuresUseNetworkError() = runTest {
        val fake = FakeSource()
        fake.failure = IOException("synthetic network failure")
        assertEquals(
            DataResult.Failure(DataError.NETWORK),
            DefaultArticleRepository(fake).articles(0)
        )
    }

    @Test
    fun malformedResponseUsesStableErrorWithoutExceptionDetails() = runTest {
        val fake = FakeSource()
        fake.failure = IllegalArgumentException("synthetic malformed payload")
        assertEquals(
            DataResult.Failure(DataError.INVALID_RESPONSE),
            DefaultArticleRepository(fake).articles(0)
        )
    }

    @Test
    fun cancellationIsNotConvertedToUiFailure() = runTest {
        val fake = FakeSource()
        fake.failure = CancellationException("superseded")
        var cancelled = false
        try {
            DefaultArticleRepository(fake).articles(0)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }
}

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
    override suspend fun articles(
        page: Int,
        categoryId: Long?
    ): WanResponse<WanPageDto<ArticleDto>> {
        failure?.let { throw it }
        requestPage = page
        return this.page
    }
    override suspend fun questions(page: Int): WanResponse<WanPageDto<ArticleDto>> {
        failure?.let { throw it }
        questionRequestPage = page
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

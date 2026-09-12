package com.personal.wanandroid.core.data

import com.personal.wanandroid.core.model.SearchHistory
import com.personal.wanandroid.core.network.SearchHotKeyDataSource
import com.personal.wanandroid.core.network.SearchHotKeyDto
import com.personal.wanandroid.core.network.WanResponse
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionsRepositoryTest {
    private val source = HotKeyFixture()
    private val repository = DefaultSearchSuggestionsRepository(HistoryFixture(), source)

    @Test fun sortsTrimsAndDeduplicatesServerKeywordsIncludingEmptyResponse() = runTest {
        source.response = WanResponse(
            0,
            data = listOf(
                SearchHotKeyDto(2, " 第二 ", 2),
                SearchHotKeyDto(1, "第一", 1),
                SearchHotKeyDto(3, "第二", 3),
                SearchHotKeyDto(4, " ", 4)
            )
        )
        assertEquals(DataResult.Success(listOf("第一", "第二")), repository.hotKeys())
        source.response = WanResponse(0, data = emptyList())
        assertEquals(DataResult.Success(emptyList<String>()), repository.hotKeys())
    }

    @Test fun businessMissingDataAndTransportErrorsAreNotEmptyRecommendations() = runTest {
        for ((response, error) in listOf(
            WanResponse<List<SearchHotKeyDto>>(-1) to DataError.SERVICE,
            WanResponse<List<SearchHotKeyDto>>(-1001) to DataError.SESSION_EXPIRED,
            WanResponse<List<SearchHotKeyDto>>(0) to DataError.INVALID_RESPONSE
        )) {
            source.response = response
            assertEquals(DataResult.Failure(error), repository.hotKeys())
        }
        source.failure = IOException("fixture")
        assertEquals(DataResult.Failure(DataError.NETWORK), repository.hotKeys())
        source.failure = CancellationException()
        var propagated = false
        try {
            repository.hotKeys()
        } catch (_: CancellationException) {
            propagated = true
        }
        assertTrue(propagated)
    }
}
private class HotKeyFixture : SearchHotKeyDataSource {
    var response: WanResponse<List<SearchHotKeyDto>> = WanResponse(0, data = emptyList())
    var failure: Exception? = null
    override suspend fun hotKeys(): WanResponse<List<SearchHotKeyDto>> {
        failure?.let { throw it }
        return response
    }
}
private class HistoryFixture : SearchHistoryDataSource {
    override val history = flowOf(SearchHistory(ready = true))
    override suspend fun record(keyword: String) = true
    override suspend fun clear() = true
}

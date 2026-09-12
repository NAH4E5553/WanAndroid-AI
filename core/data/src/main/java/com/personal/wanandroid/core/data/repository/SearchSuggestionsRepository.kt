package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.data.datasource.SearchHistoryDataSource
import com.personal.wanandroid.core.data.mapper.requestWithData
import com.personal.wanandroid.core.model.SearchHistory
import com.personal.wanandroid.core.network.datasource.SearchHotKeyDataSource
import com.personal.wanandroid.core.result.DataResult
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

interface SearchSuggestionsRepository {
    val history: Flow<SearchHistory>
    suspend fun hotKeys(): DataResult<List<String>>
    suspend fun record(keyword: String): Boolean
    suspend fun clearHistory(): Boolean
}
internal class DefaultSearchSuggestionsRepository @Inject constructor(
    private val local: SearchHistoryDataSource,
    private val network: SearchHotKeyDataSource
) : SearchSuggestionsRepository {
    override val history = local.history
    override suspend fun record(keyword: String) = local.record(keyword)
    override suspend fun clearHistory() = local.clear()
    override suspend fun hotKeys(): DataResult<List<String>> =
        requestWithData(network::hotKeys) { keys ->
            keys.sortedBy {
                it.order
            }.map { it.name.trim().take(200) }.filter { it.isNotEmpty() }.distinct()
        }
}

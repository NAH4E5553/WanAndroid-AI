package com.personal.wanandroid.core.network

import javax.inject.Inject
import kotlinx.serialization.Serializable

@Serializable
data class SearchHotKeyDto(val id: Long, val name: String, val order: Int = 0)

interface SearchHotKeyDataSource {
    suspend fun hotKeys(): WanResponse<List<SearchHotKeyDto>>
}
class RetrofitSearchHotKeyDataSource @Inject constructor(private val service: ArticleService) :
    SearchHotKeyDataSource {
    override suspend fun hotKeys() = service.hotKeys()
}

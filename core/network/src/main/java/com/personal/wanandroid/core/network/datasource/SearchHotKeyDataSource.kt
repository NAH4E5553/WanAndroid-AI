package com.personal.wanandroid.core.network.datasource

import com.personal.wanandroid.core.network.dto.SearchHotKeyDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.service.ArticleService
import javax.inject.Inject

interface SearchHotKeyDataSource {
    suspend fun hotKeys(): WanResponse<List<SearchHotKeyDto>>
}
class RetrofitSearchHotKeyDataSource @Inject constructor(private val service: ArticleService) :
    SearchHotKeyDataSource {
    override suspend fun hotKeys() = service.hotKeys()
}

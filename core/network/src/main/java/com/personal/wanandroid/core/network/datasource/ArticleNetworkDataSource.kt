package com.personal.wanandroid.core.network.datasource

import com.personal.wanandroid.core.network.dto.ArticleDto
import com.personal.wanandroid.core.network.dto.TopicDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.service.ArticleService
import javax.inject.Inject

interface ArticleNetworkDataSource {
    suspend fun articles(page: Int, categoryId: Long?): WanResponse<WanPageDto<ArticleDto>>
    suspend fun questions(page: Int): WanResponse<WanPageDto<ArticleDto>>
    suspend fun topics(): WanResponse<List<TopicDto>>
    suspend fun search(page: Int, keyword: String): WanResponse<WanPageDto<ArticleDto>>
}

class RetrofitArticleDataSource @Inject constructor(private val service: ArticleService) :
    ArticleNetworkDataSource {
    override suspend fun articles(page: Int, categoryId: Long?) = service.articles(page, categoryId)
    override suspend fun questions(page: Int) = service.questions(page)
    override suspend fun topics() = service.topics()
    override suspend fun search(page: Int, keyword: String) = service.search(page, keyword)
}

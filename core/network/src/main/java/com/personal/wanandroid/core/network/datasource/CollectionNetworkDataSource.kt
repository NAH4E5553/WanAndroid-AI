package com.personal.wanandroid.core.network.datasource

import com.personal.wanandroid.core.network.dto.CollectionDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.service.CollectionService
import com.personal.wanandroid.core.network.session.SessionRequest
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement

interface CollectionNetworkDataSource {
    suspend fun list(page: Int, session: SessionRequest): WanResponse<WanPageDto<CollectionDto>>
    suspend fun collect(articleId: Long, session: SessionRequest): WanResponse<JsonElement>
    suspend fun uncollectArticle(articleId: Long, session: SessionRequest): WanResponse<JsonElement>
    suspend fun uncollectRecord(
        recordId: Long,
        articleId: Long,
        session: SessionRequest
    ): WanResponse<JsonElement>
}

class RetrofitCollectionDataSource @Inject constructor(private val service: CollectionService) :
    CollectionNetworkDataSource {
    override suspend fun list(page: Int, session: SessionRequest) = service.list(page, session)
    override suspend fun collect(articleId: Long, session: SessionRequest) =
        service.collect(articleId, session)
    override suspend fun uncollectArticle(articleId: Long, session: SessionRequest) =
        service.uncollectArticle(articleId, session)
    override suspend fun uncollectRecord(recordId: Long, articleId: Long, session: SessionRequest) =
        service.uncollectRecord(recordId, articleId, session)
}

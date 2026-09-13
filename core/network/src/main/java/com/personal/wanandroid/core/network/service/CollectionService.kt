package com.personal.wanandroid.core.network.service

import com.personal.wanandroid.core.network.dto.CollectionDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.session.SessionRequest
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface CollectionService {
    @GET("lg/collect/list/{page}/json")
    suspend fun list(
        @Path("page") page: Int,
        @Tag session: SessionRequest,
        @Query("page_size") pageSize: Int = 20
    ): WanResponse<WanPageDto<CollectionDto>>

    @POST("lg/collect/{id}/json")
    suspend fun collect(
        @Path("id") articleId: Long,
        @Tag session: SessionRequest
    ): WanResponse<JsonElement>

    @POST("lg/uncollect_originId/{id}/json")
    suspend fun uncollectArticle(
        @Path("id") articleId: Long,
        @Tag session: SessionRequest
    ): WanResponse<JsonElement>

    @FormUrlEncoded
    @POST("lg/uncollect/{id}/json")
    suspend fun uncollectRecord(
        @Path("id") recordId: Long,
        @Field("originId") articleId: Long,
        @Tag session: SessionRequest
    ): WanResponse<JsonElement>
}

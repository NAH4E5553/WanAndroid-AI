package com.personal.wanandroid.core.network.service

import com.personal.wanandroid.core.network.dto.ArticleDto
import com.personal.wanandroid.core.network.dto.SearchHotKeyDto
import com.personal.wanandroid.core.network.dto.TopicDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ArticleService {
    @GET("hotkey/json")
    suspend fun hotKeys(): WanResponse<List<SearchHotKeyDto>>

    @GET("article/list/{page}/json")
    suspend fun articles(
        @Path("page") page: Int,
        @Query("cid") categoryId: Long? = null,
        @Query("page_size") pageSize: Int = 20
    ): WanResponse<WanPageDto<ArticleDto>>

    @GET("wenda/list/{page}/json")
    suspend fun questions(@Path("page") page: Int): WanResponse<WanPageDto<ArticleDto>>

    @GET("tree/json")
    suspend fun topics(): WanResponse<List<TopicDto>>

    @FormUrlEncoded
    @POST("article/query/{page}/json")
    suspend fun search(
        @Path("page") page: Int,
        @Field("k") keyword: String
    ): WanResponse<WanPageDto<ArticleDto>>
}

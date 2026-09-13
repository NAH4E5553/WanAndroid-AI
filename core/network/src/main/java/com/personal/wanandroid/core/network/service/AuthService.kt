package com.personal.wanandroid.core.network.service

import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.dto.UserInfoDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.session.SessionRequest
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Tag

interface AuthService {
    @FormUrlEncoded
    @POST("user/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Tag session: SessionRequest
    ): Response<WanResponse<UserDto>>

    @GET("user/lg/userinfo/json")
    suspend fun userInfo(@Tag session: SessionRequest): WanResponse<UserInfoDto>

    @GET("user/logout/json")
    suspend fun logout(@Tag session: SessionRequest): WanResponse<JsonElement>
}

package com.personal.wanandroid.core.network.datasource

import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.dto.UserInfoDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.service.AuthService
import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStore
import javax.inject.Inject
import kotlinx.serialization.json.JsonElement
import okhttp3.Cookie
import retrofit2.HttpException

// Cookie objects stay in network; the repository asks to commit only after mapping success.
class LoginResponse(val body: WanResponse<UserDto>, private val commit: (UserDto) -> Boolean) {
    fun commit(user: UserDto): Boolean = commit.invoke(user)
}
interface AuthNetworkDataSource {
    suspend fun login(username: String, password: String, session: SessionRequest): LoginResponse
    suspend fun userInfo(session: SessionRequest): WanResponse<UserInfoDto>
    suspend fun logout(session: SessionRequest): WanResponse<JsonElement>
}
class RetrofitAuthDataSource @Inject constructor(
    private val service: AuthService,
    private val sessions: SessionStore
) : AuthNetworkDataSource {
    override suspend fun login(
        username: String,
        password: String,
        session: SessionRequest
    ): LoginResponse {
        val response = service.login(username, password, session)
        if (!response.isSuccessful) throw HttpException(response)
        val body = requireNotNull(response.body())
        val cookies = Cookie.parseAll(
            requireNotNull(SessionStore.API.resolve("user/login")),
            response.headers()
        )
        return LoginResponse(body) { user -> sessions.commitLogin(session, user, cookies) }
    }
    override suspend fun userInfo(session: SessionRequest) = service.userInfo(session)
    override suspend fun logout(session: SessionRequest) = service.logout(session)
}

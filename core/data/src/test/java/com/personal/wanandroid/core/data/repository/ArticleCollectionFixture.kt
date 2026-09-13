package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.network.datasource.CollectionNetworkDataSource
import com.personal.wanandroid.core.network.dto.CollectionDto
import com.personal.wanandroid.core.network.dto.UserDto
import com.personal.wanandroid.core.network.dto.WanPageDto
import com.personal.wanandroid.core.network.dto.WanResponse
import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStorage
import com.personal.wanandroid.core.network.session.SessionStore
import kotlinx.serialization.json.JsonElement
import okhttp3.Cookie

internal class ArticleCollectionFixture {
    val sessions = SessionStore(object : SessionStorage {
        override fun read(): String? = null
        override fun write(payload: String?) = Unit
    }) { 1_000 }
    val collections = DefaultCollectionRepository(
        object : CollectionNetworkDataSource {
            override suspend fun list(page: Int, session: SessionRequest) =
                WanResponse(0, data = WanPageDto<CollectionDto>(emptyList(), 1, true, 0))
            override suspend fun collect(articleId: Long, session: SessionRequest) =
                WanResponse<JsonElement>(0)
            override suspend fun uncollectArticle(articleId: Long, session: SessionRequest) =
                WanResponse<JsonElement>(0)
            override suspend fun uncollectRecord(
                recordId: Long,
                articleId: Long,
                session: SessionRequest
            ) = WanResponse<JsonElement>(0)
        },
        sessions
    )

    fun signIn(id: Long = 7) {
        val request = sessions.beginLogin()
        check(
            sessions.commitLogin(
                request,
                UserDto(id, "fixture"),
                listOf(
                    Cookie.Builder().name(
                        "session"
                    ).value("fixture").hostOnlyDomain("wanandroid.com").expiresAt(100_000).build()
                )
            )
        )
    }
}

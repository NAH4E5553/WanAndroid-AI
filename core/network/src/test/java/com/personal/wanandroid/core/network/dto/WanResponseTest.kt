package com.personal.wanandroid.core.network.dto

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WanResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test(expected = SerializationException::class)
    fun missingErrorCodeCannotBecomeSuccess() {
        json.decodeFromString<WanResponse<String>>("""{"data":"value"}""")
    }

    @Test
    fun loginExpiryAndNullDataRemainVisible() {
        val result = json.decodeFromString<WanResponse<String>>(
            """{"errorCode":-1001,"errorMsg":"expired","data":null,"futureField":true}"""
        )
        assertEquals(-1001, result.errorCode)
        assertNull(result.data)
    }

    @Test
    fun optionalArticleFieldsMayBeNull() {
        val result = json.decodeFromString<ArticleDto>(
            """{"id":1,"title":"Example","link":"https://example.org","author":null}"""
        )
        assertNull(result.author)
        assertEquals(1L, result.id)
    }

    @Test fun loginUserIgnoresServerCredentialFieldsAndAllowsNullNickname() {
        val user = json.decodeFromString<UserDto>(
            """{
                "id":7,"username":"fixture","nickname":null,
                "password":"fixture-secret","token":"fixture-token"
            }"""
        )
        assertEquals(7L, user.id)
        assertNull(user.nickname)
        assertFalse(user.toString().contains("fixture-secret"))
        assertFalse(user.toString().contains("fixture-token"))
    }
}

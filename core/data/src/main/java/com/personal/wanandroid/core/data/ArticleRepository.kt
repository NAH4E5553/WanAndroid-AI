package com.personal.wanandroid.core.data

import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.DataError
import com.personal.wanandroid.core.model.DataResult
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.network.ArticleDto
import com.personal.wanandroid.core.network.ArticleNetworkDataSource
import com.personal.wanandroid.core.network.WanResponse
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

interface ArticleRepository {
    suspend fun articles(page: Int, categoryId: Long? = null): DataResult<PageResult<Article>>

    suspend fun questions(): DataResult<List<Article>>

    suspend fun topics(): DataResult<List<Topic>>

    suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>>
}

class DefaultArticleRepository @Inject constructor(private val source: ArticleNetworkDataSource) :
    ArticleRepository {
    override suspend fun articles(page: Int, categoryId: Long?): DataResult<PageResult<Article>> =
        request({ source.articles(page, categoryId) }) { body ->
            PageResult(body.datas.map { it.toArticle() }, if (body.over) null else page + 1)
        }

    override suspend fun questions(): DataResult<List<Article>> =
        request({ source.questions() }) { it.datas.take(5).map { dto -> dto.toArticle() } }

    override suspend fun topics(): DataResult<List<Topic>> = request({
        source.topics()
    }) { parents ->
        parents.flatMap { parent ->
            listOf(Topic(parent.id, parent.name)) +
                parent.children.map { Topic(it.id, it.name, parent.id) }
        }.distinctBy { it.id }
    }

    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        request({ source.search(page, keyword) }) { body ->
            PageResult(body.datas.map { it.toArticle() }, if (body.over) null else page + 1)
        }

    private suspend fun <T : Any, R> request(
        call: suspend () -> WanResponse<T>,
        map: (T) -> R
    ): DataResult<R> = try {
        val response = call()
        val body = response.data
        when {
            response.errorCode == -1001 -> DataResult.Failure(DataError.SESSION_EXPIRED)
            response.errorCode != 0 -> DataResult.Failure(DataError.SERVICE)
            body == null -> DataResult.Failure(DataError.INVALID_RESPONSE)
            else -> DataResult.Success(map(body))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        DataResult.Failure(DataError.NETWORK)
    } catch (_: Exception) {
        DataResult.Failure(DataError.INVALID_RESPONSE)
    }
}

private fun ArticleDto.toArticle() = Article(
    id = id,
    title = title,
    url = link,
    author = author.orEmpty(),
    shareUser = shareUser.orEmpty(),
    superChapterName = superChapterName.orEmpty(),
    chapter = chapterName.orEmpty(),
    publishedAt = niceDate.orEmpty(),
    collected = collect
)

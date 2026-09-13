package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.data.mapper.normalizeArticleLink
import com.personal.wanandroid.core.data.mapper.requestWithData
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.model.Topic
import com.personal.wanandroid.core.network.datasource.ArticleNetworkDataSource
import com.personal.wanandroid.core.network.dto.ArticleDto
import com.personal.wanandroid.core.result.DataResult
import com.personal.wanandroid.core.result.map
import javax.inject.Inject

interface ArticleRepository {
    suspend fun articles(page: Int, categoryId: Long? = null): DataResult<PageResult<Article>>

    suspend fun questions(): DataResult<List<Article>>

    suspend fun questionPage(page: Int): DataResult<PageResult<Article>>

    suspend fun topics(): DataResult<List<Topic>>

    suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>>
}

class DefaultArticleRepository @Inject constructor(
    private val source: ArticleNetworkDataSource,
    private val collections: CollectionRepository
) : ArticleRepository {
    override suspend fun articles(page: Int, categoryId: Long?): DataResult<PageResult<Article>> =
        collections.articlePage {
            requestWithData({ source.articles(page, categoryId) }) { body ->
                PageResult(body.datas.map { it.toArticle() }, if (body.over) null else page + 1)
            }
        }

    override suspend fun questions(): DataResult<List<Article>> =
        questionPage(FIRST_QUESTION_PAGE).map { it.items.take(HOME_QUESTION_LIMIT) }

    override suspend fun questionPage(page: Int): DataResult<PageResult<Article>> =
        collections.articlePage {
            requestWithData({ source.questions(page) }) { body ->
                PageResult(body.datas.map { it.toArticle() }, if (body.over) null else page + 1)
            }
        }

    override suspend fun topics(): DataResult<List<Topic>> = requestWithData({
        source.topics()
    }) { parents ->
        parents.flatMap { parent ->
            listOf(Topic(parent.id, parent.name)) +
                parent.children.map { Topic(it.id, it.name, parent.id) }
        }.distinctBy { it.id }
    }

    override suspend fun search(page: Int, keyword: String): DataResult<PageResult<Article>> =
        collections.articlePage {
            requestWithData({ source.search(page, keyword) }) { body ->
                PageResult(body.datas.map { it.toArticle() }, if (body.over) null else page + 1)
            }
        }

    private companion object {
        const val FIRST_QUESTION_PAGE = 1
        const val HOME_QUESTION_LIMIT = 5
    }
}

private fun ArticleDto.toArticle() = Article(
    id = id,
    title = title,
    url = normalizeArticleLink(link),
    author = author.orEmpty(),
    shareUser = shareUser.orEmpty(),
    superChapterName = superChapterName.orEmpty(),
    chapter = chapterName.orEmpty(),
    publishedAt = niceDate.orEmpty(),
    collected = collect
)

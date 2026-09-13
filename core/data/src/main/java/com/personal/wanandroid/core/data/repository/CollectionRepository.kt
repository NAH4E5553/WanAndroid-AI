package com.personal.wanandroid.core.data.repository

import com.personal.wanandroid.core.data.mapper.requestWithData
import com.personal.wanandroid.core.data.mapper.requestWithoutData
import com.personal.wanandroid.core.model.Article
import com.personal.wanandroid.core.model.CollectionItem
import com.personal.wanandroid.core.model.CollectionSnapshot
import com.personal.wanandroid.core.model.CollectionStatus
import com.personal.wanandroid.core.model.CollectionTarget
import com.personal.wanandroid.core.model.PageResult
import com.personal.wanandroid.core.network.datasource.CollectionNetworkDataSource
import com.personal.wanandroid.core.network.session.SessionPhase
import com.personal.wanandroid.core.network.session.SessionRequest
import com.personal.wanandroid.core.network.session.SessionStore
import com.personal.wanandroid.core.result.DataError
import com.personal.wanandroid.core.result.DataResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withTimeoutOrNull

interface CollectionRepository {
    val state: Flow<CollectionSnapshot>
    fun current(): CollectionSnapshot
    suspend fun page(generation: Long, page: Int): DataResult<PageResult<CollectionItem>>
    suspend fun reconcile(generation: Long, target: CollectionTarget): DataResult<Unit>
    suspend fun setCollected(
        generation: Long,
        target: CollectionTarget,
        collected: Boolean
    ): DataResult<Unit>
}

/** One account-scoped authority. No background scope, queued writes, or optimistic success. */
class DefaultCollectionRepository @Inject constructor(
    private val source: CollectionNetworkDataSource,
    private val sessions: SessionStore
) : CollectionRepository {
    private val lock = Any()
    private val cache = MutableStateFlow(CollectionSnapshot())
    private var writeVersion = 0L
    override val state: Flow<CollectionSnapshot> = combine(sessions.state, cache) { _, _ ->
        current()
    }.distinctUntilChanged()

    override fun current(): CollectionSnapshot = synchronized(lock) {
        val session = sessions.state.value
        val generation = session.generation.takeIf { session.phase == SessionPhase.AUTHENTICATED }
        if (cache.value.generation != generation) cache.value = CollectionSnapshot(generation)
        cache.value
    }

    private fun request(generation: Long): SessionRequest? {
        val tag = sessions.capture()
        return tag.takeIf { it.generation == generation && current().generation == generation }
    }

    private fun isCurrent(tag: SessionRequest) =
        sessions.isCurrent(tag) && current().generation == tag.generation

    private fun publish(
        tag: SessionRequest,
        target: CollectionTarget,
        status: CollectionStatus,
        changed: Boolean = false
    ) = synchronized(lock) {
        if (isCurrent(tag)) {
            val old = current()
            cache.value =
                old.copy(
                    statuses = old.statuses + (target.key to status),
                    revision =
                        old.revision + if (changed) 1 else 0
                )
        }
    }

    private fun begin(tag: SessionRequest, target: CollectionTarget): Boolean = synchronized(lock) {
        if (!isCurrent(tag) || current().status(target).busy) return@synchronized false
        publish(tag, target, current().status(target).copy(busy = true))
        true
    }

    override suspend fun page(generation: Long, page: Int): DataResult<PageResult<CollectionItem>> {
        val tag = request(generation) ?: return changed()
        val revision = synchronized(lock) { writeVersion }
        val result = readPage(tag, page)
        currentCoroutineContext().ensureActive()
        synchronized(lock) {
            if (!isCurrent(tag) || writeVersion != revision) return changed()
            if (result is DataResult.Success) {
                val old = current()
                val known = result.value.items.filter { !old.status(it.target).busy }.associate {
                    it.target.key to
                        CollectionStatus(true)
                }
                cache.value = old.copy(statuses = old.statuses + known)
            }
        }
        return result
    }

    private suspend fun readPage(
        tag: SessionRequest,
        page: Int
    ): DataResult<PageResult<CollectionItem>> {
        val result = requestWithData({ source.list(page, tag) }) { body ->
            PageResult(
                body.datas.map { dto ->
                    val target = CollectionTarget(dto.originId.takeIf { it >= 0 }, dto.id)
                    CollectionItem(
                        target,
                        Article(
                            dto.originId, dto.title, dto.link, dto.author.orEmpty(), "", "",
                            dto.chapterName.orEmpty(), dto.niceDate.orEmpty(), true
                        )
                    )
                },
                if (body.over) null else page + 1
            )
        }
        return if (isCurrent(tag)) result else changed()
    }

    override suspend fun reconcile(generation: Long, target: CollectionTarget): DataResult<Unit> {
        val tag = request(generation) ?: return changed()
        if (!begin(tag, target)) return DataResult.Success(Unit)
        return try {
            reconcileLocked(tag, target)
        } finally {
            publish(tag, target, current().status(target).copy(busy = false))
        }
    }

    /** Absence is known only after reaching the end; a bounded/failed scan stays unknown. */
    private suspend fun reconcileLocked(
        tag: SessionRequest,
        target: CollectionTarget
    ): DataResult<Unit> {
        publish(tag, target, CollectionStatus(busy = true))
        val version = synchronized(lock) { writeVersion }
        return withTimeoutOrNull(30_000) {
            for (page in 0 until 50) {
                when (val result = readPage(tag, page)) {
                    is DataResult.Failure -> return@withTimeoutOrNull result

                    is DataResult.Success -> {
                        val found = result.value.items.any { it.target.key == target.key }
                        if (found || result.value.nextPage == null) {
                            currentCoroutineContext().ensureActive()
                            if (!isCurrent(tag) ||
                                synchronized(lock) { version != writeVersion }
                            ) {
                                return@withTimeoutOrNull changed()
                            }
                            publish(tag, target, CollectionStatus(found, busy = true))
                            return@withTimeoutOrNull DataResult.Success(Unit)
                        }
                    }
                }
            }
            DataResult.Failure(DataError.NETWORK)
        } ?: DataResult.Failure(DataError.NETWORK)
    }

    override suspend fun setCollected(
        generation: Long,
        target: CollectionTarget,
        collected: Boolean
    ): DataResult<Unit> {
        val tag = request(generation) ?: return changed()
        if (!begin(tag, target)) return DataResult.Success(Unit)
        var attempted = false
        try {
            val previous = current().status(target).collected
            // An uncertain previous request can only be reconciled, never blindly replayed.
            if (previous == null) return reconcileLocked(tag, target)
            if (previous == collected) return DataResult.Success(Unit)
            if (collected && target.articleId == null) return DataResult.Failure(DataError.SERVICE)
            currentCoroutineContext().ensureActive()
            if (!isCurrent(tag)) return changed()
            attempted = true
            synchronized(lock) { writeVersion++ }
            publish(tag, target, CollectionStatus(busy = true))
            val result = requestWithoutData {
                when {
                    collected -> source.collect(requireNotNull(target.articleId), tag)

                    target.recordId != null -> source.uncollectRecord(
                        requireNotNull(target.recordId),
                        target.articleId ?: -1,
                        tag
                    )

                    else -> source.uncollectArticle(requireNotNull(target.articleId), tag)
                }
            }
            currentCoroutineContext().ensureActive()
            if (!isCurrent(tag)) return changed()
            when {
                result is DataResult.Success -> publish(
                    tag,
                    target,
                    CollectionStatus(collected, busy = true)
                )

                result is DataResult.Failure &&
                    result.reason in setOf(DataError.NETWORK, DataError.INVALID_RESPONSE) -> {
                    // The server may have committed before the connection was lost.
                    reconcileLocked(tag, target)
                }

                else -> publish(tag, target, CollectionStatus(previous, busy = true))
            }
            currentCoroutineContext().ensureActive()
            if (!isCurrent(tag)) return changed()
            return if (current().status(target).collected ==
                collected
            ) {
                DataResult.Success(Unit)
            } else {
                result
            }
        } catch (cancelled: CancellationException) {
            if (attempted) publish(tag, target, CollectionStatus(busy = true))
            throw cancelled
        } finally {
            if (attempted) synchronized(lock) { if (isCurrent(tag)) writeVersion++ }
            // Refresh collection pagination after every attempted write, including uncertain writes.
            publish(tag, target, current().status(target).copy(busy = false), changed = attempted)
        }
    }

    private fun changed() = DataResult.Failure(DataError.SESSION_CHANGED)
}

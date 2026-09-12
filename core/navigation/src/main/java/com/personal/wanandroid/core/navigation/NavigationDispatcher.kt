package com.personal.wanandroid.core.navigation

import android.os.Looper
import androidx.navigation3.runtime.NavKey
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

fun interface NavEntryIdGenerator {
    fun next(): String
}

/** Deliberately reference identity, neither serializable nor reused after reattachment. */
class NavigationHostToken internal constructor()
data class NavigationSource(val hostToken: NavigationHostToken, val entryId: String)
enum class NavigationOutcome {
    ACCEPTED,
    REJECTED_NO_HOST,
    REJECTED_STALE_HOST,
    REJECTED_STALE_SOURCE,
    REJECTED_ROOT
}

/**
 * Adapted from CoolMall AppNavigator/NavigationService: injected, one active host,
 * main-thread serialization, no pending click queue or global result broadcast.
 */
@Singleton
class NavigationDispatcher(
    private val ids: NavEntryIdGenerator,
    private val assertMainThread: () -> Unit
) {
    @Inject constructor() : this(
        NavEntryIdGenerator { UUID.randomUUID().toString() },
        { check(Looper.myLooper() == Looper.getMainLooper()) { "Navigation must run on Main" } }
    )
    private data class Host(val token: NavigationHostToken, val stack: MutableList<NavKey>)
    private var host: Host? = null

    fun createRoot(): MainRoute {
        assertMainThread()
        return MainRoute(ids.next())
    }

    fun attach(stack: MutableList<NavKey>): NavigationHostToken {
        assertMainThread()
        require(stack.isNotEmpty() && stack.all { it is AppRoute })
        val token = NavigationHostToken()
        host = Host(token, stack)
        return token
    }

    fun detach(token: NavigationHostToken) {
        assertMainThread()
        if (host?.token === token) host = null
    }

    fun navigateFrom(source: NavigationSource, destination: Destination): NavigationOutcome {
        assertMainThread()
        val rejection = rejection(source)
        if (rejection != null) return rejection
        val id = ids.next()
        val entry = when (destination) {
            Destination.Search -> SearchRoute(id)

            Destination.Questions -> DailyQuestionsRoute(id)

            Destination.Login -> LoginRoute(id)

            Destination.ThemeSettings -> ThemeSettingsRoute(id)

            is Destination.Article -> ArticleRoute(
                destination.url,
                destination.title,
                destination.articleId,
                id
            )
        }
        requireNotNull(host).stack.add(entry)
        return NavigationOutcome.ACCEPTED
    }

    fun back(source: NavigationSource): NavigationOutcome {
        assertMainThread()
        rejection(source)?.let { return it }
        return if (requireNotNull(host).stack.popIfCurrent(source.entryId)) {
            NavigationOutcome.ACCEPTED
        } else {
            NavigationOutcome.REJECTED_ROOT
        }
    }

    private fun rejection(source: NavigationSource): NavigationOutcome? {
        val current = host ?: return NavigationOutcome.REJECTED_NO_HOST
        if (source.hostToken !== current.token) return NavigationOutcome.REJECTED_STALE_HOST
        if ((current.stack.lastOrNull() as? AppRoute)?.entryId != source.entryId) {
            return NavigationOutcome.REJECTED_STALE_SOURCE
        }
        return null
    }
}

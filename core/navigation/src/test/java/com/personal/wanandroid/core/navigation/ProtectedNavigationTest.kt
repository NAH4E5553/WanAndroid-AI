package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedNavigationTest {
    private var account: Long? = null
    private var sequence = 0
    private val dispatcher =
        NavigationDispatcher(
            NavEntryIdGenerator {
                "id-${++sequence}"
            },
            {},
            AuthStateReader { account }
        )
    private val stack = mutableListOf<NavKey>(dispatcher.createRoot())
    private var host = dispatcher.attach(stack)
    private fun source() = NavigationSource(host, (stack.last() as AppRoute).entryId)

    @Test fun articleRouteCarriesListCollectAndLinkThroughRestoration() {
        dispatcher.navigateFrom(
            source(),
            Destination.Article(
                "https://fixture.invalid/article",
                "Fixture",
                42,
                collected = true,
                collectionSession = "fixture-session"
            )
        )
        val route = stack.last() as ArticleRoute
        assertEquals(true, route.collected)
        assertEquals("fixture-session", route.collectionSession)
        assertEquals("https://fixture.invalid/article", route.url)
        val encoded = Json.encodeToString<AppRoute>(route)
        assertEquals(route, Json.decodeFromString<AppRoute>(encoded))
    }

    @Test fun readingHistoryIsLocalAndRestoresWithoutLogin() {
        assertEquals(
            NavigationOutcome.ACCEPTED,
            dispatcher.navigateFrom(source(), Destination.History)
        )
        assertTrue(stack.last() is HistoryRoute)
        val encoded = Json.encodeToString(stack.map { it as AppRoute })
        assertEquals(stack.toList(), Json.decodeFromString<List<AppRoute>>(encoded))
        dispatcher.back(source())
        assertEquals(1, stack.size)
    }

    @Test fun guestTargetSurvivesRestoreAndIsConsumedOnce() {
        dispatcher.navigateFrom(source(), Destination.Collections)
        assertTrue((stack.last() as LoginRoute).pending is PendingDestination.Collections)
        val encoded = Json.encodeToString(stack.map { it as AppRoute })
        val restored = Json.decodeFromString<List<AppRoute>>(encoded)
        stack.clear()
        stack.addAll(restored)
        host = dispatcher.attach(stack)
        account = 7
        val login = source()
        assertEquals(NavigationOutcome.ACCEPTED, dispatcher.completeLogin(login))
        assertTrue(stack.last() is CollectionsRoute)
        assertEquals(2, stack.size)
        assertEquals(NavigationOutcome.REJECTED_STALE_SOURCE, dispatcher.completeLogin(login))
    }

    @Test fun cancellationDiscardsPendingAndOrdinaryLoginOnlyReturns() {
        dispatcher.navigateFrom(source(), Destination.Collections)
        dispatcher.back(source())
        dispatcher.navigateFrom(source(), Destination.Login)
        account = 7
        dispatcher.completeLogin(source())
        assertEquals(1, stack.size)
    }

    @Test fun loginCannotCompleteWithoutAccountAndAuthenticatedTargetIsDirect() {
        dispatcher.navigateFrom(source(), Destination.Collections)
        assertEquals(NavigationOutcome.REJECTED_UNAUTHENTICATED, dispatcher.completeLogin(source()))
        dispatcher.back(source())
        account = 7
        dispatcher.navigateFrom(source(), Destination.Collections)
        assertTrue(stack.last() is CollectionsRoute)
    }

    @Test fun removedOriginAndStaleHostCannotConsumePendingTarget() {
        dispatcher.navigateFrom(source(), Destination.Collections)
        val stale = source()
        host = dispatcher.attach(stack)
        account = 7
        assertEquals(NavigationOutcome.REJECTED_STALE_HOST, dispatcher.completeLogin(stale))
        stack[0] = MainRoute("replacement")
        dispatcher.completeLogin(source())
        assertEquals(listOf(MainRoute("replacement")), stack)
    }

    @Test fun switchingKnownAccountDiscardsPendingDestination() {
        val protected = NavigationDispatcher(
            NavEntryIdGenerator { "id-${++sequence}" },
            {},
            object : AuthStateReader {
                override fun authenticatedAccountId() = account
                override fun knownAccountId() = 7L
            }
        )
        val entries = mutableListOf<NavKey>(protected.createRoot())
        val token = protected.attach(entries)
        protected.navigateFrom(
            NavigationSource(token, (entries.last() as AppRoute).entryId),
            Destination.Collections
        )
        account = 8
        protected.completeLogin(NavigationSource(token, (entries.last() as AppRoute).entryId))
        assertEquals(1, entries.size)
    }
}

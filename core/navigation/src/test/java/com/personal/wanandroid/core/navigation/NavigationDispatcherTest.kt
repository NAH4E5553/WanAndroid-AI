package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDispatcherTest {
    private var sequence = 0
    private val dispatcher = NavigationDispatcher(NavEntryIdGenerator { "entry-${++sequence}" }, {})
    private val article = Destination.Article("https://example.invalid/1", "Fixture", 1)

    @Test fun doubleClickRejectsStaleSourceWithoutAllocatingAnotherEntry() {
        val stack = mutableListOf<NavKey>(dispatcher.createRoot())
        val token = dispatcher.attach(stack)
        val source = NavigationSource(token, (stack.last() as AppRoute).entryId)
        assertEquals(NavigationOutcome.ACCEPTED, dispatcher.navigateFrom(source, article))
        assertEquals(
            NavigationOutcome.REJECTED_STALE_SOURCE,
            dispatcher.navigateFrom(source, article)
        )
        assertEquals(2, stack.size)
        assertEquals(2, sequence)
    }

    @Test fun sameArticleInstancesAndDoubleBackUseIdentity() {
        val stack = mutableListOf<NavKey>(dispatcher.createRoot())
        val token = dispatcher.attach(stack)
        fun source() = NavigationSource(token, (stack.last() as AppRoute).entryId)
        dispatcher.navigateFrom(source(), article)
        val first = stack.last() as ArticleRoute
        dispatcher.navigateFrom(source(), article)
        val second = stack.last() as ArticleRoute
        assertNotEquals(first.entryId, second.entryId)
        val back = source()
        assertEquals(NavigationOutcome.ACCEPTED, dispatcher.back(back))
        assertEquals(NavigationOutcome.REJECTED_STALE_SOURCE, dispatcher.back(back))
        assertEquals(first, stack.last())
    }

    @Test fun restoredIdenticalEntryRejectsOldHostCallbackAndOldDetach() {
        val stack = mutableListOf<NavKey>(dispatcher.createRoot())
        val oldToken = dispatcher.attach(stack)
        val oldSource = NavigationSource(oldToken, (stack.last() as AppRoute).entryId)
        val restored = restore(stack)
        val newToken = dispatcher.attach(restored)
        dispatcher.detach(oldToken)
        assertEquals(
            NavigationOutcome.REJECTED_STALE_HOST,
            dispatcher.navigateFrom(oldSource, article)
        )
        assertEquals(NavigationOutcome.REJECTED_STALE_HOST, dispatcher.back(oldSource))
        val current = NavigationSource(newToken, (restored.last() as AppRoute).entryId)
        assertEquals(NavigationOutcome.ACCEPTED, dispatcher.navigateFrom(current, article))
        assertEquals(1, stack.size)
        assertEquals(2, restored.size)
    }

    @Test fun unboundClickIsRejectedAndNeverReplayed() {
        val stack = mutableListOf<NavKey>(dispatcher.createRoot())
        val token = dispatcher.attach(stack)
        val source = NavigationSource(token, (stack.last() as AppRoute).entryId)
        dispatcher.detach(token)
        assertEquals(NavigationOutcome.REJECTED_NO_HOST, dispatcher.navigateFrom(source, article))
        dispatcher.attach(stack)
        assertEquals(1, stack.size)
    }

    @Test fun allEntriesSerializeWithStableIdentityAndRootCannotBePopped() {
        val stack = mutableListOf<NavKey>(dispatcher.createRoot())
        val token = dispatcher.attach(stack)
        val rootSource = NavigationSource(token, (stack.last() as AppRoute).entryId)
        assertEquals(NavigationOutcome.REJECTED_ROOT, dispatcher.back(rootSource))
        for (destination in listOf(
            Destination.Search,
            Destination.Questions,
            Destination.Login,
            Destination.ThemeSettings,
            article
        )) {
            val source = NavigationSource(token, (stack.last() as AppRoute).entryId)
            dispatcher.navigateFrom(source, destination)
        }
        assertEquals(stack, restore(stack))
        assertTrue(stack.map { (it as AppRoute).entryId }.distinct().size == stack.size)
    }

    @Test fun stackMutationRequiresMainThreadCheck() {
        val guarded = NavigationDispatcher(NavEntryIdGenerator { "id" }, { error("not main") })
        val outcome = runCatching { guarded.createRoot() }
        assertTrue(outcome.isFailure)
    }

    private fun restore(stack: List<NavKey>): MutableList<NavKey> {
        val json = Json.encodeToString(stack.map { it as AppRoute })
        return Json.decodeFromString<List<AppRoute>>(json).toMutableList<NavKey>()
    }
}

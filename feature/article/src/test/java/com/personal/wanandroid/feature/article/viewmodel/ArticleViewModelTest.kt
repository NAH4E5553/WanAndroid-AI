package com.personal.wanandroid.feature.article.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.personal.wanandroid.core.navigation.ArticleRoute
import com.personal.wanandroid.feature.article.state.ReaderEvent
import com.personal.wanandroid.feature.article.state.ReaderFailure
import com.personal.wanandroid.feature.article.state.ReaderNotice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val url = "https://reader.invalid/one"

    @Before fun before() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun after() {
        Dispatchers.resetMain()
    }
    private fun model(saved: SavedStateHandle = SavedStateHandle(), target: String = url) =
        ArticleViewModel(
            saved,
            ArticleRoute(url = target, title = "Fixture", articleId = 1L, entryId = "fixture")
        )

    @Test fun successClampsProgressAndKeepsTitleForEmptyWebTitle() = runTest(dispatcher) {
        val vm = model()
        val id = vm.attachBrowser()
        vm.onBrowserEvent(id, ReaderEvent.Started(url))
        vm.onBrowserEvent(id, ReaderEvent.Progress(url, 80))
        vm.onBrowserEvent(id, ReaderEvent.Progress(url, -2))
        assertEquals(80, vm.uiState.value.progress)
        vm.onBrowserEvent(id, ReaderEvent.Progress(url, 200))
        assertEquals(100, vm.uiState.value.progress)
        vm.onBrowserEvent(id, ReaderEvent.Finished(url, "  "))
        assertFalse(vm.uiState.value.loading)
        assertEquals("Fixture", vm.uiState.value.title)
        advanceTimeBy(31_000)
        runCurrent()
        assertNull(vm.uiState.value.failure)
    }

    @Test fun failureSurvivesLateFinishAndOnlyRetryStartsNewBrowser() = runTest(dispatcher) {
        val vm = model()
        val old = vm.attachBrowser()
        vm.onBrowserEvent(old, ReaderEvent.Failed(url, ReaderFailure.TLS))
        vm.onBrowserEvent(old, ReaderEvent.Finished(url, "Error page"))
        assertEquals(ReaderFailure.TLS, vm.uiState.value.failure)
        vm.retry()
        val fresh = vm.attachBrowser()
        vm.detachBrowser(old)
        vm.onBrowserEvent(old, ReaderEvent.RendererGone)
        vm.onBrowserEvent(old, ReaderEvent.External("tel:123"))
        vm.onBrowserEvent(fresh, ReaderEvent.Finished(url, "Ready"))
        assertNull(vm.uiState.value.failure)
        assertNull(vm.uiState.value.pendingExternal)
        assertEquals("Ready", vm.uiState.value.title)
        assertEquals(1L, vm.uiState.value.reloadId)
    }

    @Test fun callbacksForPreviousUrlDoNotCompleteCurrentNavigation() = runTest(dispatcher) {
        val vm = model()
        val id = vm.attachBrowser()
        val next = "https://reader.invalid/two"
        vm.onBrowserEvent(id, ReaderEvent.Started(next))
        vm.onBrowserEvent(id, ReaderEvent.Failed(url, ReaderFailure.NETWORK))
        vm.onBrowserEvent(id, ReaderEvent.Finished(url, "Old"))
        assertTrue(vm.uiState.value.loading)
        assertNull(vm.uiState.value.failure)
        vm.onBrowserEvent(id, ReaderEvent.Finished(next, "New"))
        assertEquals("New", vm.uiState.value.title)
    }

    @Test fun stalledLoadTimesOutAndDetachCancelsItsDeadline() = runTest(dispatcher) {
        val vm = model()
        vm.attachBrowser()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(ReaderFailure.TIMEOUT, vm.uiState.value.failure)
        vm.retry()
        val id = vm.attachBrowser()
        vm.detachBrowser(id)
        advanceTimeBy(30_000)
        runCurrent()
        assertNull(vm.uiState.value.failure)
    }

    @Test fun restorationSavesOnlyCurrentSafeUrl() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val vm = model(saved)
        val id = vm.attachBrowser()
        vm.onBrowserEvent(id, ReaderEvent.History("https://reader.invalid/two#part", true))
        assertEquals(setOf("reader.url"), saved.keys())
        assertEquals("https://reader.invalid/two#part", model(saved).uiState.value.url)
        assertEquals(
            url,
            model(SavedStateHandle(mapOf("reader.url" to "javascript:evil"))).uiState.value.url
        )
        vm.detachBrowser(id)
    }

    @Test fun externalIntentIsConfirmedOnceAndCancelledOnNavigation() = runTest(dispatcher) {
        val vm = model()
        val id = vm.attachBrowser()
        vm.requestExternal("tel:123")
        assertEquals("tel:123", vm.consumeExternal())
        assertNull(vm.consumeExternal())
        vm.requestExternal("mailto:test@example.invalid")
        vm.dismissExternal()
        assertNull(vm.consumeExternal())
        vm.requestExternal("tel:123")
        vm.onBrowserEvent(id, ReaderEvent.Started(url))
        assertNull(vm.consumeExternal())
        vm.requestExternal("intent://evil")
        assertEquals(ReaderNotice.BLOCKED_LINK, vm.uiState.value.notice)
        assertNull(vm.consumeExternal())
        vm.detachBrowser(id)
    }

    @Test fun rendererFailureCanRecoverAndRejectsLateFinish() = runTest(dispatcher) {
        val vm = model()
        val id = vm.attachBrowser()
        vm.onBrowserEvent(id, ReaderEvent.History(url, true))
        vm.onBrowserEvent(id, ReaderEvent.RendererGone)
        vm.onBrowserEvent(id, ReaderEvent.Finished(url, "Late"))
        assertEquals(ReaderFailure.RENDERER, vm.uiState.value.failure)
        assertFalse(vm.uiState.value.canGoBack)
        vm.retry()
        val next = vm.attachBrowser()
        vm.onBrowserEvent(next, ReaderEvent.Finished(url, "Recovered"))
        assertNull(vm.uiState.value.failure)
    }

    @Test fun invalidInitialUrlHasNoInertRetry() {
        val vm = model(target = "file:///private/file")
        vm.retry()
        assertEquals(ReaderFailure.UNSUPPORTED_URL, vm.uiState.value.failure)
        assertEquals(0L, vm.uiState.value.reloadId)
    }
}

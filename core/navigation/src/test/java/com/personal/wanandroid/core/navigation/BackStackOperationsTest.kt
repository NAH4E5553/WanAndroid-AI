package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackStackOperationsTest {
    @Test
    fun duplicateBackNeverRemovesRoot() {
        val stack = mutableListOf<NavKey>(MainRoute, SearchRoute)
        assertTrue(stack.popIfCurrent(SearchRoute))
        assertFalse(stack.popIfCurrent(SearchRoute))
        assertEquals(listOf(MainRoute), stack)
    }

    @Test
    fun staleCallbackCannotPopAnotherDestination() {
        val stack = mutableListOf<NavKey>(MainRoute, LoginRoute)
        assertFalse(stack.popIfCurrent(SearchRoute))
        assertEquals(LoginRoute, stack.last())
    }

    @Test
    fun rootCannotBePopped() {
        val stack = mutableListOf<NavKey>(MainRoute)
        assertFalse(stack.popIfCurrent(MainRoute))
    }
}

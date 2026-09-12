package com.personal.wanandroid.core.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackStackOperationsTest {
    @Test
    fun duplicateBackNeverRemovesRoot() {
        val stack = mutableListOf<NavKey>(MainRoute("root"), SearchRoute("search"))
        assertTrue(stack.popIfCurrent("search"))
        assertFalse(stack.popIfCurrent("search"))
        assertEquals(listOf(MainRoute("root")), stack)
    }

    @Test
    fun staleCallbackCannotPopAnotherDestination() {
        val stack = mutableListOf<NavKey>(MainRoute("root"), LoginRoute("login"))
        assertFalse(stack.popIfCurrent("search"))
        assertEquals(LoginRoute("login"), stack.last())
    }

    @Test
    fun rootCannotBePopped() {
        val stack = mutableListOf<NavKey>(MainRoute("root"))
        assertFalse(stack.popIfCurrent("root"))
    }
}

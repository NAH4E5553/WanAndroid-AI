package com.personal.wanandroid.feature.auth.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberPolicyTest {
    @Test fun acceptsElevenAsciiDigitsAndTrimsOuterWhitespace() {
        for (prefix in '3'..'9') {
            assertTrue(isMainlandMobileNumber("1${prefix}000000000"))
        }
        assertTrue(isMainlandMobileNumber(" 13800000000 "))
    }

    @Test fun rejectsEmptyWrongPrefixWrongLengthAndNonAsciiInput() {
        for (value in listOf(
            "", " ", "1380000000", "138000000000", "12800000000", "23800000000",
            "+8613800000000", "138 00000000", "1380000000a", "１３８００００００００"
        )) {
            assertFalse(value, isMainlandMobileNumber(value))
        }
    }
}

package com.personal.wanandroid.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DataResultTest {
    @Test fun mapsSuccessAndPreservesFailureWithoutCallingTransform() {
        assertEquals(DataResult.Success(4), DataResult.Success(2).map { it * 2 })
        var called = false
        val failure: DataResult<Int> = DataResult.Failure(DataError.NETWORK)
        assertEquals(
            failure,
            failure.map {
                called = true
                it * 2
            }
        )
        assertFalse(called)
    }
}

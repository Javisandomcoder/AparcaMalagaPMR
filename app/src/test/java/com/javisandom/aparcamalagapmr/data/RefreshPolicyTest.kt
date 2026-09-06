package com.javisandom.aparcamalagapmr.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {
    @Test
    fun `refresh is due when no successful update exists`() {
        assertTrue(isParkingRefreshDue(lastSuccessMillis = null, nowMillis = 100_000L))
    }

    @Test
    fun `refresh is not due before twenty four hours`() {
        assertFalse(isParkingRefreshDue(lastSuccessMillis = 1_000L, nowMillis = 86_400_999L))
    }

    @Test
    fun `refresh is due after twenty four hours`() {
        assertTrue(isParkingRefreshDue(lastSuccessMillis = 1_000L, nowMillis = 86_401_000L))
    }
}

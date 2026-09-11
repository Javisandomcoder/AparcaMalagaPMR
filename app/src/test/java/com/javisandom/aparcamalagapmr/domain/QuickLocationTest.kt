package com.javisandom.aparcamalagapmr.domain

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class QuickLocationTest {
    private val now = 1_000_000L
    private val fix = LocationFix(UserLocation(36.72, -4.4), now)

    @Test fun `fresh cached position returns without waiting for providers`() = runBlocking {
        assertEquals(fix, firstFreshLocation(fix, listOf({ error("Must not wait") }), { now }))
    }

    @Test fun `network can win while GPS is still waiting and GPS gets cancelled`() = runBlocking {
        val gpsStarted = CompletableDeferred<Unit>()
        var gpsCancelled = false
        val result = firstFreshLocation(null, listOf(
            { try { gpsStarted.complete(Unit); awaitCancellation() } finally { gpsCancelled = true } },
            { gpsStarted.await(); fix },
        ), { now })
        assertEquals(fix, result)
        assertTrue(gpsCancelled)
    }

    @Test fun `stale positions are ignored while another provider can succeed`() = runBlocking {
        assertEquals(fix, firstFreshLocation(fix.copy(capturedAtMillis = 1), listOf(
            { fix.copy(capturedAtMillis = 1) }, { fix },
        ), { now }))
    }

    @Test fun `timeout cancels outstanding provider`() = runBlocking {
        var cancelled = false
        val result = firstFreshLocation(null, listOf({
            try { awaitCancellation() } finally { cancelled = true }
        }), { now }, timeoutMillis = 100)
        assertNull(result)
        assertTrue(cancelled)
    }
}

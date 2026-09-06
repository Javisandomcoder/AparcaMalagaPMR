package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.*
import org.junit.Test

class LocationFreshnessTest {
    private val now = 1_000_000L
    private val location = UserLocation(36.72, -4.42)

    @Test fun `old missing and future timestamps are rejected`() {
        for (timestamp in listOf<Long?>(null, now - 300_001L, now + 1L)) {
            assertNull(selectFreshLocation(listOf(LocationFix(location, timestamp)), true, now))
        }
    }

    @Test fun `permission denial excludes even a fresh stored location`() {
        assertNull(selectFreshLocation(listOf(LocationFix(location, now)), false, now))
    }

    @Test fun `newest valid location wins regardless of source order`() {
        val old = LocationFix(location, now - 300_000L)
        val recent = LocationFix(UserLocation(36.73, -4.43), now)
        assertEquals(recent, selectFreshLocation(listOf(old, recent), true, now))
        assertEquals(old, selectFreshLocation(listOf(old), true, now))
    }

    @Test fun `invalid coordinates are ignored`() {
        assertNull(selectFreshLocation(listOf(LocationFix(UserLocation(Double.NaN, -4.42), now)), true, now))
        assertNull(selectFreshLocation(listOf(LocationFix(UserLocation(91.0, -4.42), now)), true, now))
    }
}

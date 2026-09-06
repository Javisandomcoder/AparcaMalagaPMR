package com.javisandom.aparcamalagapmr.data

import androidx.test.platform.app.InstrumentationRegistry
import com.javisandom.aparcamalagapmr.domain.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationStoreTest {
    @Test
    fun saves_and_loads_the_last_user_location() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = LocationStore(context)
        store.clear()
        assertNull(store.load())

        store.save(UserLocation(36.7213, -4.4214))

        assertEquals(UserLocation(36.7213, -4.4214), store.load())
        store.clear()
    }

    @Test
    fun old_and_legacy_locations_are_not_restored() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var now = 1_000_000L
        val store = LocationStore(context) { now }
        store.clear()
        try {
            store.save(UserLocation(36.7213, -4.4214))
            now += 300_001L
            assertNull(store.load())
            store.save(UserLocation(36.7213, -4.4214))
            context.getSharedPreferences("last_user_location", android.content.Context.MODE_PRIVATE)
                .edit().remove("captured_at_millis").commit()
            assertNull(store.load())
        } finally {
            store.clear()
        }
    }
}

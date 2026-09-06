package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.*
import org.junit.Test

class MovingParkingMapTest {
    private val west = ParkingSpot("west", "Test west", 36.72, -4.44)
    private val east = ParkingSpot("east", "Test east", 36.72, -4.40)

    @Test fun `moving changes nearby markers but preserves selection`() {
        val map = MovingParkingMap(listOf(west, east))
        map.updateLocation(UserLocation(west.latitude, west.longitude))
        assertEquals("west", map.markers.first().id)
        map.select(west)
        map.updateLocation(UserLocation(east.latitude, east.longitude))
        assertEquals("east", map.markers.first().id)
        assertEquals(west, map.selected)
    }

    @Test fun `small gps movement does not recalculate marker set`() {
        val map = MovingParkingMap(listOf(west, east))
        map.updateLocation(UserLocation(36.72, -4.44))
        val before = map.markers
        map.updateLocation(UserLocation(36.72001, -4.44))
        assertSame(before, map.markers)
        assertEquals(UserLocation(36.72001, -4.44), map.origin)
    }

    @Test fun `exploration stops camera following until recentered`() {
        val map = MovingParkingMap(listOf(west, east))
        map.updateLocation(UserLocation(36.72, -4.44))
        map.explore(UserLocation(36.75, -4.43))
        map.updateLocation(UserLocation(36.72, -4.40))
        assertEquals(UserLocation(36.75, -4.43), map.center)
        map.recenter()
        assertEquals(map.origin, map.center)
    }

    @Test fun `lost location removes vehicle position but keeps map and selection`() {
        val map = MovingParkingMap(listOf(west, east))
        map.updateLocation(UserLocation(36.72, -4.44))
        map.select(west)
        val center = map.center
        map.updateLocation(null)
        assertNull(map.origin)
        assertEquals(center, map.center)
        assertEquals(west, map.selected)
    }

    @Test fun `projection round trips and east is right north is up`() {
        val origin = UserLocation(36.72, -4.42)
        val point = MapProjection.project(origin, 16)
        val restored = MapProjection.unproject(point, 16)
        assertEquals(origin.latitude, restored.latitude, 0.000001)
        assertEquals(origin.longitude, restored.longitude, 0.000001)
        assertTrue(MapProjection.project(UserLocation(36.73, -4.42), 16).y < point.y)
        assertTrue(MapProjection.project(UserLocation(36.72, -4.41), 16).x > point.x)
    }
}

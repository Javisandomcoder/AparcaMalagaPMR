package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.*
import org.junit.Test

class MapHeadingTest {
    @Test fun `isolated reverse fix does not spin the map`() {
        val heading = MapHeading()
        heading.update(UserLocation(36.72, -4.44))
        heading.update(UserLocation(36.72, -4.439))
        val east = heading.degrees
        heading.update(UserLocation(36.72, -4.44))
        assertEquals(east, heading.degrees, 0.1)
    }
    @Test fun `sustained turn is accepted gradually rather than as a right angle jump`() {
        val heading = MapHeading()
        heading.update(UserLocation(36.72, -4.44))
        heading.update(UserLocation(36.72, -4.439))
        heading.update(UserLocation(36.721, -4.439))
        assertEquals(90.0, heading.degrees, 0.1)
        heading.update(UserLocation(36.722, -4.439))
        assertTrue(heading.degrees in 20.0..80.0)
        repeat(8) { heading.update(UserLocation(36.723 + it * .001, -4.439)) }
        assertTrue(kotlin.math.abs(shortestHeadingDelta(heading.degrees, 0.0)) < 8.0)
    }

    @Test fun `eastward travel turns map but small jitter does not`() {
        val heading = MapHeading()
        heading.update(UserLocation(36.72, -4.44))
        heading.update(UserLocation(36.72, -4.43))
        assertEquals(90.0, heading.degrees, 0.1)
        heading.update(UserLocation(36.720001, -4.43))
        assertEquals(90.0, heading.degrees, 0.1)
    }
    @Test fun `missing location resets bearing reference`() {
        val heading = MapHeading()
        heading.update(UserLocation(36.72, -4.44)); heading.update(null)
        heading.update(UserLocation(36.72, -4.40))
        assertEquals(0.0, heading.degrees, 0.0)
    }
    @Test fun `heading animation takes shortest path across north`() {
        assertEquals(2.0, shortestHeadingDelta(359.0, 1.0), 0.0)
        assertEquals(-2.0, shortestHeadingDelta(1.0, 359.0), 0.0)
    }
}

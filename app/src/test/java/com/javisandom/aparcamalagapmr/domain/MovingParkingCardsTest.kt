package com.javisandom.aparcamalagapmr.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MovingParkingCardsTest {
    // Synthetic coordinates are test fixtures only.
    private val south = UserLocation(36.72, -4.42)
    private val north = UserLocation(36.73, -4.42)
    private val a = ParkingSpot("a", "A", south.latitude, south.longitude)
    private val b = ParkingSpot("b", "B", north.latitude, north.longitude)

    @Test fun `moving changes order and brings new nearby spots into the cards`() {
        val cards = MovingParkingCards(listOf(a, b), maxItems = 1)
        assertEquals(listOf(a), cards.update(south, 0))
        assertEquals(listOf(b), cards.update(north, 10_000))
    }

    @Test fun `reordering waits ten seconds and then uses the latest location`() {
        val cards = MovingParkingCards(listOf(a, b))
        cards.update(south, 0)
        assertEquals(listOf(a, b), cards.update(north, 9_999))
        assertEquals(listOf(b, a), cards.update(north, 10_000))
    }

    @Test fun `small GPS movement does not reorder nearly equidistant cards`() {
        val cards = MovingParkingCards(listOf(a, b))
        val midpoint = UserLocation(36.725, -4.42)
        cards.update(UserLocation(36.7248, -4.42), 0)
        assertEquals(listOf(a, b), cards.update(UserLocation(midpoint.latitude + 0.0002, -4.42), 20_000))
    }

    @Test fun `near ties retain order even after substantial sideways movement`() {
        val cards = MovingParkingCards(listOf(a, b))
        cards.update(UserLocation(36.72499, -4.42), 0)
        assertEquals(listOf(a, b), cards.update(UserLocation(36.72501, -4.418), 10_000))
        assertEquals(listOf(b, a), cards.update(north, 20_000))
    }

    @Test fun `first fix immediately replaces alphabetical order and losing location preserves cards`() {
        val cards = MovingParkingCards(listOf(a, b))
        assertEquals(listOf(a, b), cards.update(null, 0))
        assertEquals(listOf(b, a), cards.update(north, 1))
        assertEquals(listOf(b, a), cards.update(null, 20_000))
    }

    @Test fun `explicit refresh bypasses time and distance gates`() {
        val cards = MovingParkingCards(listOf(a, b))
        cards.update(south, 0)
        assertEquals(listOf(b, a), cards.update(north, 1, force = true))
        assertEquals(emptyList<ParkingSpot>(), MovingParkingCards(emptyList()).update(north, 0))
    }
}

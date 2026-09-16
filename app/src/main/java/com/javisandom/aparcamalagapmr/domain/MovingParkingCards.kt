package com.javisandom.aparcamalagapmr.domain

/** Selection state for the vehicle-relative cards, independent of the car UI. */
class MovingParkingCards(private val spots: List<ParkingSpot>, private val maxItems: Int = 6) {
    private var selected: List<ParkingSpot>? = null
    private var lastOrigin: UserLocation? = null
    private var lastSelectionMillis = 0L

    fun update(origin: UserLocation?, elapsedMillis: Long, force: Boolean = false): List<ParkingSpot> {
        if (origin == null) {
            if (selected == null) selected = spotsForCar(spots, maxItems)
            return selected!!
        }
        val previousOrigin = lastOrigin
        if (!force && previousOrigin != null) {
            val moved = distanceMeters(previousOrigin,
                ParkingSpot("selection-origin", "", origin.latitude, origin.longitude))
            if (elapsedMillis - lastSelectionMillis < 10_000 || moved < 50) return selected!!
        }

        val distances = spots.associate { it.id to distanceMeters(origin, it) }
        val remaining = spots.sortedWith(compareBy<ParkingSpot> { distances.getValue(it.id) }.thenBy { it.id })
            .toMutableList()
        val previous = if (force || previousOrigin == null) emptyList() else selected.orEmpty()
        selected = buildList {
            repeat(maxItems.coerceAtMost(remaining.size)) { index ->
                val nearest = remaining.first()
                val incumbent = previous.getOrNull(index)?.takeIf { it in remaining }
                val next = if (incumbent != null &&
                    distances.getValue(incumbent.id) < distances.getValue(nearest.id) + 25
                ) incumbent else nearest
                add(next)
                remaining.remove(next)
            }
        }
        lastOrigin = origin
        lastSelectionMillis = elapsedMillis
        return selected!!
    }
}

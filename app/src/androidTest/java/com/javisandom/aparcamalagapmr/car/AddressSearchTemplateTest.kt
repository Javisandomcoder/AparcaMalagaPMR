package com.javisandom.aparcamalagapmr.car

import androidx.car.app.model.*
import com.javisandom.aparcamalagapmr.domain.*
import org.junit.Assert.*
import org.junit.Test

class AddressSearchTemplateTest {
    private val callback = object : SearchTemplate.SearchCallback {}
    private val spot = ParkingSpot("77785", "PASEO CERRADO DE CALDERÓN, 18", 36.72615397, -4.38156389)
    @Test fun nearby_is_available_even_when_search_is_loading_or_has_no_results() {
        for (loading in listOf(true, false)) {
            val template = addressSearchTemplate("Larios", loading, ParkingSearchResult(emptyList(), null),
                5, callback, {}, {}, onNearby = {})
            assertEquals("Cerca de mí", template.actionStrip!!.actions.single().title!!.toCharSequence().toString())
        }
    }
    @Test fun searched_cards_and_detail_keep_the_address_as_distance_origin() {
        val origin = UserLocation(36.72, -4.42)
        val cards = parkingListTemplate(listOf(spot), origin, canGoBack = true,
            addressLabel = "Calle Larios", onNearby = {}) {}
        assertEquals(Action.BACK, cards.headerAction)
        assertEquals("Cerca de mí", cards.actionStrip!!.actions.single().title!!.toCharSequence().toString())
        val detail = parkingDetailTemplate(spot, origin, addressLabel = "Calle Larios") {}
        assertEquals("${formatDistance(distanceMeters(origin, spot))} desde Calle Larios · en línea recta",
            detail.pane.rows.first().texts.last().toCharSequence().toString())
        assertEquals("Navegar", detail.pane.actions.single().title!!.toCharSequence().toString())
    }
    @Test fun loading_has_back_and_no_stale_results() {
        val template = addressSearchTemplate("Larios", true, ParkingSearchResult(listOf(spot), null), 5, callback, {}, {})
        assertTrue(template.isLoading); assertNull(template.itemList); assertEquals(Action.BACK, template.headerAction)
    }
    @Test fun results_identify_destination_distance_and_offer_cards_with_host_limit() {
        val result = ParkingSearchResult(listOf(spot, spot), UserLocation(36.72, -4.42), "Dirección buscada")
        val template = addressSearchTemplate("Dirección", false, result, 1, callback, {}, {}, onNearby = {})
        assertEquals(1, template.itemList!!.items.size)
        val row = template.itemList!!.items.first() as Row
        assertTrue(row.texts.first().toCharSequence().toString().contains("Dirección buscada"))
        assertEquals("Ver tarjetas", template.actionStrip!!.actions.first().title!!.toCharSequence().toString())
        val map = parkingListTemplate(result.spots, result.origin, canGoBack = true, addressLabel = result.address) {}
        assertTrue(map.title!!.toCharSequence().toString().contains("Dirección buscada"))
        assertNotNull((map.itemList!!.items.first() as Row).metadata!!.place!!.marker)
    }
}

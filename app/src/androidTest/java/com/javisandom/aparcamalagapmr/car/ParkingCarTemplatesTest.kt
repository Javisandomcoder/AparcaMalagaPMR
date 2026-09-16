package com.javisandom.aparcamalagapmr.car

import android.text.Spanned
import androidx.car.app.model.DistanceSpan
import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import com.javisandom.aparcamalagapmr.domain.UserLocation
import org.junit.Assert.*
import org.junit.Test

class ParkingCarTemplatesTest {
    @Test fun home_cards_make_search_and_location_reference_explicit() {
        val template = parkingListTemplate(spots, null, onSearch = {}, rankedCards = true) {}
        assertEquals("Plazas cerca de mí", template.title!!.toCharSequence().toString())
        assertEquals("Buscar dirección", template.actionStrip!!.actions.single().title!!.toCharSequence().toString())
    }

    @Test fun reordered_cards_keep_refresh_titles_but_change_address_and_marker() {
        val other = ParkingSpot("test", "OTRA DIRECCIÓN DE PRUEBA", 36.72, -4.42)
        val first = parkingListTemplate(spots + other, null, rankedCards = true) {}
        val next = parkingListTemplate(listOf(other) + spots, UserLocation(36.72, -4.42), rankedCards = true) {}
        val oldRows = first.itemList!!.items.map { it as androidx.car.app.model.Row }
        val newRows = next.itemList!!.items.map { it as androidx.car.app.model.Row }
        assertEquals(first.title, next.title)
        assertEquals(oldRows.map { it.title }, newRows.map { it.title })
        assertEquals(other.displayName, newRows.first().texts.first().toCharSequence().toString())
        assertEquals(other.latitude, newRows.first().metadata!!.place!!.location.latitude, 0.0)
        assertTrue(newRows.all { it.texts.size <= 2 })
    }

    @Test fun secondary_list_has_back_in_every_state_but_root_keeps_app_icon() {
        assertEquals(androidx.car.app.model.Action.BACK, parkingListTemplate(spots, null, canGoBack = true) {}.headerAction)
        assertEquals(androidx.car.app.model.Action.BACK, parkingLoadingTemplate(true).headerAction)
        assertEquals(androidx.car.app.model.Action.BACK, parkingErrorTemplate(true) {}.headerAction)
        assertEquals(androidx.car.app.model.Action.APP_ICON, parkingListTemplate(spots, null) {}.headerAction)
    }

    @Test fun each_card_supplies_a_parking_marker_at_its_real_coordinates() {
        val row = parkingListTemplate(spots, null) {}.itemList!!.items.single() as androidx.car.app.model.Row
        val place = row.metadata!!.place!!
        assertNotNull(place.marker)
        assertEquals("P", place.marker!!.label!!.toCharSequence().toString())
        assertEquals(spots.single().latitude, place.location.latitude, 0.0)
        assertEquals(spots.single().longitude, place.location.longitude, 0.0)
    }

    @Test fun moving_map_keeps_its_surface_template_during_loading() {
        val template = movingMapLoadingTemplate()
        assertTrue(template is androidx.car.app.navigation.model.MapWithContentTemplate)
        val content = (template as androidx.car.app.navigation.model.MapWithContentTemplate).contentTemplate
        assertTrue((content as androidx.car.app.model.MessageTemplate).isLoading)
    }
    private val spots = listOf(ParkingSpot("77785", "PASEO CERRADO DE CALDERÓN, 18", 36.72615397, -4.38156389))

    @Test fun list_without_location_is_browsable_without_invented_distances() {
        val template = parkingListTemplate(spots, null) {}
        val row = template.itemList!!.items.single() as androidx.car.app.model.Row
        assertTrue(row.isBrowsable)
        assertTrue(row.texts.none { it.toCharSequence().toString().contains(" km") })
    }

    @Test fun list_with_location_has_a_host_formatted_distance() {
        val template = parkingListTemplate(spots, UserLocation(36.72, -4.42)) {}
        val row = template.itemList!!.items.single() as androidx.car.app.model.Row
        assertTrue(row.texts.any {
            val text = it.toCharSequence()
            text is Spanned && text.getSpans(0, text.length, DistanceSpan::class.java).isNotEmpty()
        })
    }

    @Test fun loading_error_and_detail_templates_are_valid() {
        assertTrue(parkingLoadingTemplate().isLoading)
        assertEquals(1, parkingErrorTemplate {}.actions.size)
        val detail = parkingDetailTemplate(spots.single()) {}
        assertEquals(1, detail.pane.actions.size)
        assertTrue(detail.pane.rows.any { it.title?.toCharSequence()?.toString() == "Sin disponibilidad en tiempo real" })
    }

    @Test fun empty_list_is_valid() {
        assertTrue(parkingListTemplate(emptyList(), null) {}.itemList!!.items.isEmpty())
    }

    @Test fun moving_map_uses_stable_content_and_host_controls() {
        val first = movingMapTemplate({}, {}, {}, {})
        val next = movingMapTemplate({}, {}, {}, {})
        assertEquals(first.contentTemplate, next.contentTemplate)
        assertEquals(3, first.actionStrip!!.actions.size)
        assertEquals(3, first.mapController!!.mapActionStrip!!.actions.size)
    }

    @Test fun detail_shows_distance_only_with_a_known_origin() {
        val withLocation = parkingDetailTemplate(spots.single(), UserLocation(36.72615397, -4.38056389)) {}
        assertTrue(withLocation.pane.rows.first().texts.any { it.toCharSequence().toString() == "89 m · en línea recta" })
        val withoutLocation = parkingDetailTemplate(spots.single()) {}
        assertEquals(1, withoutLocation.pane.rows.first().texts.size)
    }
}

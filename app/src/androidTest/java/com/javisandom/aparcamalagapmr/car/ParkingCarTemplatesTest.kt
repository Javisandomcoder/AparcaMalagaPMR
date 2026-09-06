package com.javisandom.aparcamalagapmr.car

import android.text.Spanned
import androidx.car.app.model.DistanceSpan
import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import com.javisandom.aparcamalagapmr.domain.UserLocation
import org.junit.Assert.*
import org.junit.Test

class ParkingCarTemplatesTest {
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
        assertEquals(2, first.actionStrip!!.actions.size)
        assertEquals(3, first.mapController!!.mapActionStrip!!.actions.size)
    }

    @Test fun detail_shows_distance_only_with_a_known_origin() {
        val withLocation = parkingDetailTemplate(spots.single(), UserLocation(36.72615397, -4.38056389)) {}
        assertTrue(withLocation.pane.rows.first().texts.any { it.toCharSequence().toString() == "89 m · en línea recta" })
        val withoutLocation = parkingDetailTemplate(spots.single()) {}
        assertEquals(1, withoutLocation.pane.rows.first().texts.size)
    }
}

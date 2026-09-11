package com.javisandom.aparcamalagapmr

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.onAllNodesWithText
import com.javisandom.aparcamalagapmr.domain.ResolvedAddress
import kotlinx.coroutines.delay
import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import com.javisandom.aparcamalagapmr.domain.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AparcaMalagaAppTest {
    @Test fun address_without_spaces_shows_nearby_places_and_nearby_button_returns_to_device() {
        var requested = false
        composeRule.setContent {
            AparcaMalagaApp(spots = spots, onRequestLocation = { requested = true },
                resolveAddress = { ResolvedAddress("Destino resuelto", UserLocation(36.72615397, -4.38056389)) })
        }
        composeRule.onNodeWithText("Buscar por calle o zona").performTextInput("dirección sin plaza")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Cerca de: Destino resuelto").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("89 m").assertIsDisplayed()
        composeRule.onNodeWithText("Ordenar por cercanía").performClick()
        assertEquals(true, requested)
        composeRule.onNodeWithText("Buscar por calle o zona").assertIsDisplayed()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Cerca de: Destino resuelto").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test fun typing_new_query_discards_pending_address_lookup() {
        composeRule.setContent {
            AparcaMalagaApp(spots = spots, resolveAddress = {
                delay(1_000)
                ResolvedAddress("Dirección anterior", UserLocation(36.72, -4.4))
            })
        }
        val search = composeRule.onNodeWithText("Buscar por calle o zona")
        search.performTextInput("sin coincidencias")
        search.performTextClearance()
        search.performTextInput("calderon")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("PASEO CERRADO DE CALDERÓN, 18").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, composeRule.onAllNodesWithText("Cerca de: Dirección anterior").fetchSemanticsNodes().size)
    }
    @get:Rule
    val composeRule = createComposeRule()

    private val spots = listOf(
        ParkingSpot(
            id = "77785",
            address = "PASEO CERRADO DE CALDERÓN, 18",
            latitude = 36.72615397,
            longitude = -4.38156389,
            description = "Junto al número 18",
            spaceCount = 2,
        ),
    )

    @Test
    fun home_shows_municipal_spot_and_opens_navigation() {
        var navigatedTo: ParkingSpot? = null
        composeRule.setContent {
            AparcaMalagaApp(spots = spots, onNavigate = { navigatedTo = it })
        }

        composeRule.onNodeWithText("PASEO CERRADO DE CALDERÓN, 18").assertIsDisplayed()
        composeRule.onNodeWithText("2 plazas PMR").assertIsDisplayed()
        composeRule.onNodeWithText("Navegar").performClick()

        assertEquals("77785", navigatedTo?.id)
    }

    @Test
    fun search_filters_by_address_without_accents() {
        composeRule.setContent {
            AparcaMalagaApp(spots = spots, onNavigate = {})
        }

        composeRule.onNodeWithText("Buscar por calle o zona").performTextInput("calderon")
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("PASEO CERRADO DE CALDERÓN, 18").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("PASEO CERRADO DE CALDERÓN, 18").assertIsDisplayed()
    }

    @Test
    fun location_can_be_requested_without_blocking_the_list() {
        var requested = false
        composeRule.setContent {
            AparcaMalagaApp(
                spots = spots,
                onRequestLocation = { requested = true },
                onNavigate = {},
            )
        }

        composeRule.onNodeWithText("Ordenar por cercanía").performClick()

        assertEquals(true, requested)
        composeRule.onNodeWithText("PASEO CERRADO DE CALDERÓN, 18").assertIsDisplayed()
    }

    @Test
    fun location_displays_distance_and_nearby_state() {
        composeRule.setContent {
            AparcaMalagaApp(
                spots = spots,
                userLocation = UserLocation(36.72615397, -4.38056389),
                onNavigate = {},
            )
        }

        composeRule.onNodeWithText("Ordenadas por cercanía").assertIsDisplayed()
        composeRule.onNodeWithText("89 m").assertIsDisplayed()
    }

    @Test
    fun manual_refresh_can_be_requested() {
        var requested = false
        composeRule.setContent {
            AparcaMalagaApp(
                spots = spots,
                onRefresh = { requested = true },
                onNavigate = {},
            )
        }

        composeRule.onNodeWithText("Actualizar datos").performClick()

        assertEquals(true, requested)
    }
}

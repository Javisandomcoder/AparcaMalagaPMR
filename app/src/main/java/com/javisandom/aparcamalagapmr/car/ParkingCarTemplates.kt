package com.javisandom.aparcamalagapmr.car

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.model.Action
import androidx.car.app.model.CarLocation
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.CarColor
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.Row
import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import com.javisandom.aparcamalagapmr.domain.UserLocation
import com.javisandom.aparcamalagapmr.domain.distanceMeters
import com.javisandom.aparcamalagapmr.domain.formatDistance

internal fun parkingListTemplate(
    spots: List<ParkingSpot>,
    origin: UserLocation?,
    onRefresh: (() -> Unit)? = null,
    canGoBack: Boolean = false,
    addressLabel: String? = null,
    onSearch: (() -> Unit)? = null,
    rankedCards: Boolean = false,
    onNearby: (() -> Unit)? = null,
    onSelect: (ParkingSpot) -> Unit,
): PlaceListMapTemplate {
    val items = ItemList.Builder().setNoItemsMessage("No hay plazas municipales disponibles.")
    spots.forEachIndexed { index, spot ->
        val row = Row.Builder()
            // Position titles remain stable across host refreshes; the actual place is in line 1.
            .setTitle(if (rankedCards) "${index + 1}. Aparcamiento PMR" else spot.displayName)
            .addText(if (rankedCards) spot.displayName else
                "${spot.spaceCount} ${if (spot.spaceCount == 1) "plaza" else "plazas"} PMR")
            .setMetadata(Metadata.Builder().setPlace(
                Place.Builder(CarLocation.create(spot.latitude, spot.longitude))
                    .setMarker(PlaceMarker.Builder().setLabel("P").setColor(CarColor.BLUE).build())
                    .build(),
            ).build())
            .setBrowsable(true)
            .setOnClickListener { onSelect(spot) }
        if (origin != null && addressLabel != null) {
            row.addText("${formatDistance(distanceMeters(origin, spot))} desde la dirección · en línea recta")
        } else if (origin != null) {
            val distance = SpannableString(if (rankedCards)
                "  · ${spot.spaceCount} ${if (spot.spaceCount == 1) "plaza" else "plazas"} PMR · en línea recta" else " ")
            distance.setSpan(
                DistanceSpan.create(Distance.create(distanceMeters(origin, spot), Distance.UNIT_METERS)),
                0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            row.addText(distance)
        } else {
            row.addText("Sin ubicación reciente")
        }
        items.addItem(row.build())
    }
    val builder = PlaceListMapTemplate.Builder()
        .setTitle(addressLabel?.let { "Cerca de $it" } ?: "Plazas cerca de mí")
        .setHeaderAction(if (canGoBack) Action.BACK else Action.APP_ICON)
        .setCurrentLocationEnabled(false)
        .setItemList(items.build())
    if (onRefresh != null) builder.setOnContentRefreshListener { onRefresh() }
    val actions = androidx.car.app.model.ActionStrip.Builder()
    if (onSearch != null) actions.addAction(Action.Builder().setTitle("Buscar dirección")
        .setOnClickListener { onSearch() }.build())
    if (onNearby != null) actions.addAction(Action.Builder().setTitle("Cerca de mí")
        .setOnClickListener { onNearby() }.build())
    if (onSearch != null || onNearby != null) builder.setActionStrip(actions.build())
    return builder.build()
}

internal fun parkingLoadingTemplate(canGoBack: Boolean = false): PlaceListMapTemplate = PlaceListMapTemplate.Builder()
    .setTitle("Plazas cerca de mí")
    .setHeaderAction(if (canGoBack) Action.BACK else Action.APP_ICON)
    .setLoading(true)
    .build()

// Header.Builder requires Car API 7; retain compatibility with the declared minimum (1).
@Suppress("DEPRECATION")
internal fun parkingErrorTemplate(canGoBack: Boolean = false, onRetry: () -> Unit): MessageTemplate = MessageTemplate.Builder(
    "No se pudieron cargar las plazas municipales. Inténtalo de nuevo.",
).setTitle("Plazas cerca de mí")
    .setHeaderAction(if (canGoBack) Action.BACK else Action.APP_ICON)
    .addAction(Action.Builder().setTitle("Reintentar").setOnClickListener { onRetry() }.build())
    .build()

@Suppress("DEPRECATION") // The replacement header API requires Car API 7.
internal fun parkingDetailTemplate(spot: ParkingSpot, origin: UserLocation? = null,
    addressLabel: String? = null, onNavigate: () -> Unit): PaneTemplate {
    val locationRow = Row.Builder().setTitle(spot.displayName)
        .addText("${spot.spaceCount} ${if (spot.spaceCount == 1) "plaza" else "plazas"} PMR")
    if (origin != null) locationRow.addText("${formatDistance(distanceMeters(origin, spot))}" +
        (addressLabel?.let { " desde $it" } ?: "") + " · en línea recta")
    val pane = Pane.Builder()
        .addRow(locationRow.build())
        .addRow(Row.Builder().setTitle("Sin disponibilidad en tiempo real")
            .addText("Fuente: Ayuntamiento de Málaga — Datos abiertos").build())
        .addAction(Action.Builder().setTitle("Navegar").setOnClickListener { onNavigate() }.build())
        .build()
    return PaneTemplate.Builder(pane).setTitle("Plaza PMR").setHeaderAction(Action.BACK).build()
}

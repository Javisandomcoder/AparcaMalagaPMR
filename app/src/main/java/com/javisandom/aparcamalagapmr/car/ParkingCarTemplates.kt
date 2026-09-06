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
    onSelect: (ParkingSpot) -> Unit,
): PlaceListMapTemplate {
    val items = ItemList.Builder().setNoItemsMessage("No hay plazas municipales disponibles.")
    spots.forEach { spot ->
        val row = Row.Builder()
            .setTitle(spot.displayName)
            .addText("${spot.spaceCount} ${if (spot.spaceCount == 1) "plaza" else "plazas"} PMR")
            .setMetadata(Metadata.Builder().setPlace(
                Place.Builder(CarLocation.create(spot.latitude, spot.longitude)).build(),
            ).build())
            .setBrowsable(true)
            .setOnClickListener { onSelect(spot) }
        if (origin != null) {
            val distance = SpannableString(" ")
            distance.setSpan(
                DistanceSpan.create(Distance.create(distanceMeters(origin, spot), Distance.UNIT_METERS)),
                0, distance.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            row.addText(distance)
        } else {
            row.addText("Sin ubicación reciente")
        }
        items.addItem(row.build())
    }
    val builder = PlaceListMapTemplate.Builder()
        .setTitle("Plazas PMR de Málaga")
        .setHeaderAction(Action.APP_ICON)
        .setCurrentLocationEnabled(false)
        .setItemList(items.build())
    if (onRefresh != null) builder.setOnContentRefreshListener { onRefresh() }
    return builder.build()
}

internal fun parkingLoadingTemplate(): PlaceListMapTemplate = PlaceListMapTemplate.Builder()
    .setTitle("Plazas PMR de Málaga")
    .setHeaderAction(Action.APP_ICON)
    .setLoading(true)
    .build()

// Header.Builder requires Car API 7; retain compatibility with the declared minimum (1).
@Suppress("DEPRECATION")
internal fun parkingErrorTemplate(onRetry: () -> Unit): MessageTemplate = MessageTemplate.Builder(
    "No se pudieron cargar las plazas municipales. Inténtalo de nuevo.",
).setTitle("Plazas PMR de Málaga")
    .setHeaderAction(Action.APP_ICON)
    .addAction(Action.Builder().setTitle("Reintentar").setOnClickListener { onRetry() }.build())
    .build()

@Suppress("DEPRECATION") // The replacement header API requires Car API 7.
internal fun parkingDetailTemplate(spot: ParkingSpot, origin: UserLocation? = null, onNavigate: () -> Unit): PaneTemplate {
    val locationRow = Row.Builder().setTitle(spot.displayName)
        .addText("${spot.spaceCount} ${if (spot.spaceCount == 1) "plaza" else "plazas"} PMR")
    if (origin != null) locationRow.addText("${formatDistance(distanceMeters(origin, spot))} · en línea recta")
    val pane = Pane.Builder()
        .addRow(locationRow.build())
        .addRow(Row.Builder().setTitle("Sin disponibilidad en tiempo real")
            .addText("Fuente: Ayuntamiento de Málaga — Datos abiertos").build())
        .addAction(Action.Builder().setTitle("Navegar").setOnClickListener { onNavigate() }.build())
        .build()
    return PaneTemplate.Builder(pane).setTitle("Plaza PMR").setHeaderAction(Action.BACK).build()
}

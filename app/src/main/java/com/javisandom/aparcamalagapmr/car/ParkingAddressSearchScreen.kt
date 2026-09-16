package com.javisandom.aparcamalagapmr.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.*
import androidx.lifecycle.lifecycleScope
import com.javisandom.aparcamalagapmr.data.AddressResolver
import com.javisandom.aparcamalagapmr.data.createParkingRepository
import com.javisandom.aparcamalagapmr.domain.*
import kotlinx.coroutines.*

internal class ParkingAddressSearchScreen(carContext: CarContext) : Screen(carContext) {
    private val resolver = AddressResolver(carContext)
    private var query = ""
    private var result = ParkingSearchResult(emptyList(), null, message = "Busca una dirección de Málaga.")
    private var loading = false
    private var searchJob: Job? = null
    private var catalogue: List<ParkingSpot>? = null
    private fun search(text: String, submitted: Boolean) {
        query = text
        searchJob?.cancel()
        loading = text.trim().length >= 3
        result = ParkingSearchResult(emptyList(), null, message = "Escribe una dirección de Málaga.")
        invalidate()
        if (!loading) return
        searchJob = lifecycleScope.launch {
            if (!submitted) delay(600)
            try {
                val spots = catalogue ?: withContext(Dispatchers.IO) {
                    createParkingRepository(carContext).loadCachedOrBundled()
                }.also { catalogue = it }
                result = searchParkingNearAddress(spots, text, resolver::resolve)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                result = ParkingSearchResult(emptyList(), null, message = "No se pudieron cargar las plazas. Vuelve a buscar.")
            }
            loading = false
            invalidate()
            if (submitted && result.spots.isNotEmpty()) showCards(result)
        }
    }
    private fun showCards(value: ParkingSearchResult) {
        screenManager.push(AddressParkingResultsScreen(carContext, value))
    }
    override fun onGetTemplate(): Template = addressSearchTemplate(query, loading, result,
        carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST),
        object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) = search(searchText, false)
            override fun onSearchSubmitted(searchText: String) = search(searchText, true)
        }, onCards = { showCards(result) }, onSelect = {
            screenManager.push(ParkingDetailScreen(carContext, it, result.origin, result.address))
        }, onNearby = { screenManager.popToRoot() })
}

internal fun addressSearchTemplate(query: String, loading: Boolean, result: ParkingSearchResult, limit: Int,
    callback: SearchTemplate.SearchCallback, onCards: () -> Unit, onSelect: (ParkingSpot) -> Unit,
    onNearby: (() -> Unit)? = null): SearchTemplate {
    val builder = SearchTemplate.Builder(callback).setHeaderAction(Action.BACK)
        .setSearchHint("Dirección en Málaga").setInitialSearchText(query)
        .setShowKeyboardByDefault(query.isBlank()).setLoading(loading)
    if (!loading) {
        val items = ItemList.Builder().setNoItemsMessage(result.message ?: "No hay plazas municipales disponibles.")
        result.spots.take(limit.coerceAtLeast(1)).forEach { spot ->
            val distance = result.origin?.let { formatDistance(distanceMeters(it, spot)) }
            items.addItem(Row.Builder().setTitle(spot.displayName)
                .addText("${distance ?: "—"} desde ${result.address ?: "la dirección"} · en línea recta")
                .setBrowsable(true).setOnClickListener { onSelect(spot) }.build())
        }
        builder.setItemList(items.build())
    }
    val actions = ActionStrip.Builder()
    if (!loading && result.spots.isNotEmpty()) {
        actions.addAction(Action.Builder().setTitle("Ver tarjetas").setOnClickListener { onCards() }.build())
    }
    // SearchTemplate permits only one text action; results already provide a route back home.
    if (onNearby != null && (loading || result.spots.isEmpty())) {
        actions.addAction(Action.Builder().setTitle("Cerca de mí").setOnClickListener { onNearby() }.build())
    }
    if ((!loading && result.spots.isNotEmpty()) || onNearby != null) {
        builder.setActionStrip(actions.build())
    }
    return builder.build()
}

internal class AddressParkingResultsScreen(carContext: CarContext, private val result: ParkingSearchResult) : Screen(carContext) {
    override fun onGetTemplate(): Template = parkingListTemplate(
        result.spots.take(carContext.getCarService(ConstraintManager::class.java)
            .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)),
        result.origin, canGoBack = true, addressLabel = result.address,
        onNearby = { screenManager.popToRoot() },
        onSelect = { screenManager.push(ParkingDetailScreen(carContext, it, result.origin, result.address)) })
}

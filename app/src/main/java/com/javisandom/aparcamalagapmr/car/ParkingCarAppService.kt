package com.javisandom.aparcamalagapmr.car

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.javisandom.aparcamalagapmr.BuildConfig
import com.javisandom.aparcamalagapmr.data.createParkingRepository
import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import com.javisandom.aparcamalagapmr.domain.UserLocation
import com.javisandom.aparcamalagapmr.domain.navigationUriFor
import com.javisandom.aparcamalagapmr.domain.MovingParkingCards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParkingCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        if (BuildConfig.DEBUG) {
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            releaseHostValidator(applicationContext)
        }

    override fun onCreateSession(): Session = ParkingSession()
}

internal fun releaseHostValidator(context: android.content.Context): HostValidator =
    HostValidator.Builder(context)
        // Android Auto may not hold the privileged TEMPLATE_RENDERER permission.
        // Trust the official host certificates while keeping other hosts blocked.
        .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
        .build()

private class ParkingSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        ParkingHomeScreen(carContext)
}

internal class ParkingHomeScreen(carContext: CarContext, private val canGoBack: Boolean = false) : Screen(carContext) {
    private val loader = CarParkingLoader {
        withContext(Dispatchers.IO) {
            createParkingRepository(carContext).loadCachedOrBundled()
        }
    }
    private var loadJob: Job? = null
    private var origin: UserLocation? = null
    private var visibleSpots = emptyList<ParkingSpot>()
    private var cards: MovingParkingCards? = null
    private val locationUpdates = CarLocationUpdates(carContext) { /* Read the latest fix on the bounded UI tick. */ }

    init {
        lifecycleScope.launch {
            loader.state.collect { state ->
                if (state is CarParkingState.Ready) {
                    val limit = if (carContext.carAppApiLevel >= 2) {
                        carContext.getCarService(androidx.car.app.constraints.ConstraintManager::class.java)
                            .getContentLimit(androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
                    } else 6
                    cards = MovingParkingCards(state.spots, maxItems = limit.coerceIn(1, 6))
                    updateCards(force = true)
                }
                invalidate()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    while (isActive) {
                        locationUpdates.start()
                        updateCards()
                        delay(5_000L)
                    }
                } finally {
                    locationUpdates.stop()
                }
            }
        }
        load()
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = lifecycleScope.launch { loader.load() }
    }

    private fun updateCards(force: Boolean = false) {
        val latest = CarLocationProvider.lastKnown(carContext)
        val selected = cards?.update(latest, SystemClock.elapsedRealtime(), force).orEmpty()
        val changed = latest != origin || selected != visibleSpots
        origin = latest
        visibleSpots = selected
        if (changed || force) invalidate()
    }

    override fun onGetTemplate(): Template = when (loader.state.value) {
        CarParkingState.Loading -> parkingLoadingTemplate(canGoBack)
        CarParkingState.Error -> parkingErrorTemplate(canGoBack, onRetry = ::load)
        is CarParkingState.Ready -> parkingListTemplate(
            spots = visibleSpots,
            rankedCards = true,
            canGoBack = canGoBack,
            onSearch = { screenManager.push(ParkingAddressSearchScreen(carContext)) },
            origin = origin,
            onRefresh = if (carContext.carAppApiLevel >= 5) {
                { updateCards(force = true) }
            } else null,
            onSelect = { spot -> screenManager.push(ParkingDetailScreen(carContext, spot)) },
        )
    }
}

internal class ParkingDetailScreen(
    carContext: CarContext,
    private val spot: ParkingSpot,
    private val searchOrigin: UserLocation? = null,
    private val addressLabel: String? = null,
) : Screen(carContext) {
    override fun onGetTemplate(): Template = parkingDetailTemplate(
        spot, if (addressLabel != null) searchOrigin else CarLocationProvider.lastKnown(carContext), addressLabel,
    ) {
        carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse(navigationUriFor(spot))))
    }
}

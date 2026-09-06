package com.javisandom.aparcamalagapmr.car

import android.content.Intent
import android.net.Uri
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
import com.javisandom.aparcamalagapmr.domain.spotsForCar
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
            HostValidator.Builder(applicationContext).build()
        }

    override fun onCreateSession(): Session = ParkingSession()
}

private class ParkingSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen =
        if (carContext.carAppApiLevel >= 7) MovingParkingScreen(carContext) else ParkingHomeScreen(carContext)
}

internal class ParkingHomeScreen(carContext: CarContext) : Screen(carContext) {
    private val loader = CarParkingLoader {
        withContext(Dispatchers.IO) {
            createParkingRepository(carContext).loadCachedOrBundled()
        }
    }
    private var loadJob: Job? = null
    private var origin: UserLocation? = null
    private var visibleSpots = emptyList<ParkingSpot>()

    init {
        lifecycleScope.launch {
            loader.state.collect { state ->
                if (state is CarParkingState.Ready) selectSpots(state.spots)
                invalidate()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    val latest = CarLocationProvider.lastKnown(carContext)
                    if (latest != origin) {
                        origin = latest
                        // Keep row titles/order stable: automatic updates must respect template quotas.
                        // A host content-refresh request explicitly selects a new set of nearby spots.
                        invalidate()
                    }
                    delay(30_000L)
                }
            }
        }
        load()
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = lifecycleScope.launch { loader.load() }
    }

    private fun selectSpots(spots: List<ParkingSpot>) {
        origin = CarLocationProvider.lastKnown(carContext)
        visibleSpots = spotsForCar(spots, origin = origin)
    }

    override fun onGetTemplate(): Template = when (val state = loader.state.value) {
        CarParkingState.Loading -> parkingLoadingTemplate()
        CarParkingState.Error -> parkingErrorTemplate(::load)
        is CarParkingState.Ready -> parkingListTemplate(
            spots = visibleSpots,
            origin = origin,
            onRefresh = if (carContext.carAppApiLevel >= 5) {
                { selectSpots(state.spots); invalidate() }
            } else null,
            onSelect = { spot -> screenManager.push(ParkingDetailScreen(carContext, spot)) },
        )
    }
}

internal class ParkingDetailScreen(
    carContext: CarContext,
    private val spot: ParkingSpot,
) : Screen(carContext) {
    override fun onGetTemplate(): Template = parkingDetailTemplate(spot, CarLocationProvider.lastKnown(carContext)) {
        carContext.startCarApp(Intent(CarContext.ACTION_NAVIGATE, Uri.parse(navigationUriFor(spot))))
    }
}

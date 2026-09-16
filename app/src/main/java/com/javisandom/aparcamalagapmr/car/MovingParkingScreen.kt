package com.javisandom.aparcamalagapmr.car

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.javisandom.aparcamalagapmr.data.DeviceLocationSource
import com.javisandom.aparcamalagapmr.data.createParkingRepository
import com.javisandom.aparcamalagapmr.domain.MovingParkingMap
import kotlinx.coroutines.*

internal class MovingParkingScreen(carContext: CarContext) : Screen(carContext) {
    private val source = DeviceLocationSource(carContext)
    private val loader = CarParkingLoader {
        withContext(Dispatchers.IO) { createParkingRepository(carContext).loadCachedOrBundled() }
    }
    private var model: MovingParkingMap? = null
    private var renderer: ParkingMapSurface? = null
    private var updates: CarLocationUpdates? = null
    private var refreshJob: Job? = null
    private var loadJob: Job? = null
    private var visible = false

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { visible = true; attach() }
            override fun onStop(owner: LifecycleOwner) { visible = false; detach() }
        })
        lifecycleScope.launch {
            loader.state.collect {
                if (it is CarParkingState.Ready) {
                    model = MovingParkingMap(it.spots)
                    if (visible) attach()
                }
                invalidate()
            }
        }
        load()
    }
    private fun load() {
        if (loadJob?.isActive == true) return
        loadJob = lifecycleScope.launch { loader.load() }
    }
    private fun attach() {
        android.util.Log.d("ParkingSurface", "attach visible=$visible model=${model != null}")
        val map = model ?: return
        detach()
        renderer = ParkingMapSurface(carContext, map, lifecycleScope, isDarkMode = { carContext.isDarkMode }) {
            screenManager.push(ParkingDetailScreen(carContext, it))
        }.also { carContext.getCarService(AppManager::class.java).setSurfaceCallback(it) }
        updates = CarLocationUpdates(carContext, ::updateLocation).also { it.start() }
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                updates?.start()
                updateLocation()
                delay(5_000)
            }
        }
    }
    private fun updateLocation() {
        val fix = source.latestFix()
        val granted = ContextCompat.checkSelfPermission(carContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val status = when {
            !granted -> "Activa el permiso en el teléfono"
            fix == null -> "Buscando ubicación"
            System.currentTimeMillis() - (fix.capturedAtMillis ?: 0) > 20_000 -> "Última ubicación disponible"
            else -> "Siguiendo tu ubicación"
        }
        renderer?.update(fix?.location, status)
    }
    private fun detach() {
        android.util.Log.d("ParkingSurface", "detach")
        refreshJob?.cancel()
        refreshJob = null
        updates?.stop()
        updates = null
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(null)
        renderer?.close()
        renderer = null
    }

    override fun onGetTemplate(): Template = when (loader.state.value) {
        CarParkingState.Loading -> movingMapLoadingTemplate()
        CarParkingState.Error -> parkingErrorTemplate(onRetry = ::load)
        is CarParkingState.Ready -> movingMapTemplate(
            onCenter = { renderer?.recenter() },
            onList = { screenManager.push(ParkingHomeScreen(carContext, canGoBack = true)) },
            onZoomIn = { renderer?.changeZoom(1) },
            onZoomOut = { renderer?.changeZoom(-1) },
            onSearch = { screenManager.push(ParkingAddressSearchScreen(carContext)) },
        )
    }
}

internal fun movingMapLoadingTemplate(): MapWithContentTemplate = MapWithContentTemplate.Builder()
    .setContentTemplate(MessageTemplate.Builder("Cargando plazas PMR…").setLoading(true)
        .setHeader(Header.Builder().setTitle("Plazas PMR").setStartHeaderAction(Action.APP_ICON).build())
        .build())
    .build()

/** This content remains stable while the surface moves, preserving the host's step quota. */
internal fun movingMapTemplate(
    onCenter: () -> Unit,
    onList: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSearch: () -> Unit = {},
): MapWithContentTemplate {
    val content = MessageTemplate.Builder("Busca una dirección o elige una plaza en el mapa.")
        .setHeader(Header.Builder().setTitle("Plazas PMR") .setStartHeaderAction(Action.APP_ICON).build())
        .build()
    return MapWithContentTemplate.Builder()
        .setContentTemplate(content)
        .setActionStrip(ActionStrip.Builder()
            .addAction(Action.Builder().setTitle("Buscar").setOnClickListener { onSearch() }.build())
            .addAction(Action.Builder().setTitle("Seguir vehículo").setOnClickListener { onCenter() }.build())
            .addAction(Action.Builder().setTitle("Listado").setOnClickListener { onList() }.build())
            .build())
        .setMapController(MapController.Builder().setMapActionStrip(ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(Action.Builder().setIcon(mapControlIcon("+")) .setOnClickListener { onZoomIn() }.build())
            .addAction(Action.Builder().setIcon(mapControlIcon("−")) .setOnClickListener { onZoomOut() }.build())
            .build()).build())
        .build()
}

private fun mapControlIcon(text: String): CarIcon {
    val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
    Canvas(bitmap).drawText(text, 24f, 37f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        textAlign = Paint.Align.CENTER
    })
    return CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build()
}

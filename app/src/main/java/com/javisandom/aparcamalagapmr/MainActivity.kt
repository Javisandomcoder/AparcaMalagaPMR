package com.javisandom.aparcamalagapmr

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.javisandom.aparcamalagapmr.data.DeviceLocationSource
import com.javisandom.aparcamalagapmr.data.LocationStore
import com.javisandom.aparcamalagapmr.data.ParkingRefreshResult
import com.javisandom.aparcamalagapmr.data.ParkingRepository
import com.javisandom.aparcamalagapmr.data.createParkingRepository
import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import com.javisandom.aparcamalagapmr.domain.UserLocation
import com.javisandom.aparcamalagapmr.domain.distanceMeters
import com.javisandom.aparcamalagapmr.domain.formatDistance
import com.javisandom.aparcamalagapmr.domain.navigationUriFor
import com.javisandom.aparcamalagapmr.domain.searchParkingSpots
import com.javisandom.aparcamalagapmr.domain.sortParkingSpotsByDistance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var locationCancellation: CancellationSignal? = null
    private lateinit var locationStore: LocationStore
    private val deviceLocationSource by lazy { DeviceLocationSource(applicationContext) }
    private lateinit var parkingRepository: ParkingRepository
    private val userLocation = mutableStateOf<UserLocation?>(null)
    private val locationMessage = mutableStateOf<String?>(null)
    private val parkingSpots = mutableStateOf<List<ParkingSpot>>(emptyList())
    private val dataMessage = mutableStateOf("Cargando datos…")
    private val isLoading = mutableStateOf(true)
    private val isRefreshing = mutableStateOf(false)
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) {
            loadCurrentLocation()
        } else {
            locationMessage.value = "Ubicación denegada. Puedes seguir buscando por calle."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationStore = LocationStore(applicationContext)
        parkingRepository = createParkingRepository(applicationContext)
        userLocation.value = deviceLocationSource.latest()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    userLocation.value = deviceLocationSource.latest()
                    delay(30_000L)
                }
            }
        }
        setContent {
            AparcaMalagaApp(
                spots = parkingSpots.value,
                userLocation = userLocation.value,
                locationMessage = locationMessage.value,
                dataMessage = dataMessage.value,
                isLoading = isLoading.value,
                isRefreshing = isRefreshing.value,
                onRequestLocation = ::requestLocation,
                onRefresh = { refreshParkingData(force = true) },
                onNavigate = ::openNavigation,
            )
        }
        loadParkingData()
    }

    private fun loadParkingData() {
        lifecycleScope.launch {
            try {
                parkingSpots.value = withContext(Dispatchers.IO) {
                    parkingRepository.loadCachedOrBundled()
                }
                dataMessage.value = "disponibles sin conexión"
                isLoading.value = false
                refreshParkingData(force = false)
            } catch (_: Exception) {
                dataMessage.value = "no se pudieron cargar los datos"
                isLoading.value = false
            }
        }
    }

    private fun refreshParkingData(force: Boolean) {
        if (isRefreshing.value) return
        lifecycleScope.launch {
            isRefreshing.value = true
            when (val result = withContext(Dispatchers.IO) {
                parkingRepository.refresh(force)
            }) {
                is ParkingRefreshResult.Updated -> {
                    parkingSpots.value = result.spots
                    dataMessage.value = "actualizados ahora"
                }
                ParkingRefreshResult.Skipped -> Unit
                is ParkingRefreshResult.Failed -> {
                    dataMessage.value = "sin conexión; se conserva la última copia"
                }
            }
            isRefreshing.value = false
        }
    }

    private fun requestLocation() {
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (coarseGranted) {
            loadCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    private fun loadCurrentLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            locationMessage.value = "Activa la ubicación del dispositivo para ordenar por cercanía."
            return
        }
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val providers = if (fineGranted) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        val provider = providers.firstOrNull {
            LocationManagerCompat.hasProvider(locationManager, it) && locationManager.isProviderEnabled(it)
        }
        if (provider == null) {
            locationMessage.value = "No hay un proveedor de ubicación disponible."
            return
        }
        locationCancellation?.cancel()
        val cancellation = CancellationSignal()
        locationCancellation = cancellation
        locationMessage.value = "Obteniendo ubicación…"
        try {
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellation,
                ContextCompat.getMainExecutor(this),
            ) { location ->
                if (location == null) {
                    locationMessage.value = "No se ha podido obtener la ubicación. Inténtalo de nuevo."
                } else {
                    val obtained = UserLocation(location.latitude, location.longitude)
                    locationStore.save(obtained, location.time)
                    userLocation.value = deviceLocationSource.latest()
                    locationMessage.value = null
                }
            }
        } catch (_: SecurityException) {
            locationMessage.value = "No hay permiso para acceder a la ubicación."
        }
    }

    override fun onStop() {
        locationCancellation?.cancel()
        locationCancellation = null
        locationMessage.value = null
        super.onStop()
    }

    private fun openNavigation(spot: ParkingSpot) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(navigationUriFor(spot)))
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${spot.latitude},${spot.longitude}"),
                ),
            )
        }
    }
}

@Composable
fun AparcaMalagaApp(
    spots: List<ParkingSpot> = emptyList(),
    userLocation: UserLocation? = null,
    locationMessage: String? = null,
    dataMessage: String = "copia local disponible",
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    onRequestLocation: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onNavigate: (ParkingSpot) -> Unit = {},
) {
    var query by remember { mutableStateOf("") }
    val visibleSpots = remember(spots, query, userLocation) {
        val filtered = searchParkingSpots(spots, query)
        userLocation?.let { sortParkingSpotsByDistance(filtered, it) } ?: filtered
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = "Aparcamientos PMR de Málaga",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${spots.size} ubicaciones municipales · $dataMessage",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
            ) { scaffoldPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(scaffoldPadding),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Buscar por calle o zona") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(
                        text = "La ubicación no indica disponibilidad en tiempo real.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(
                        onClick = onRequestLocation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(if (userLocation == null) "Ordenar por cercanía" else "Ordenadas por cercanía")
                    }
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(if (isRefreshing) "Actualizando…" else "Actualizar datos")
                    }
                    if (locationMessage != null) {
                        Text(
                            text = locationMessage,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (visibleSpots.isEmpty()) {
                        Text(
                            text = if (isLoading) {
                                "Cargando plazas municipales…"
                            } else if (spots.isEmpty()) {
                                "No se han podido cargar las plazas municipales."
                            } else {
                                "No hay resultados para esta búsqueda."
                            },
                            modifier = Modifier.padding(24.dp),
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(visibleSpots, key = { it.id }) { spot ->
                                ParkingSpotCard(
                                    spot = spot,
                                    userLocation = userLocation,
                                    onNavigate = { onNavigate(spot) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkingSpotCard(
    spot: ParkingSpot,
    userLocation: UserLocation?,
    onNavigate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = spot.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${spot.spaceCount} ${if (spot.spaceCount == 1) "plaza" else "plazas"} PMR",
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            if (userLocation != null) {
                Text(
                    text = formatDistance(distanceMeters(userLocation, spot)),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (spot.description.isNotBlank() && spot.description != "No hay más datos") {
                Text(
                    text = spot.description,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onNavigate) {
                    Text("Navegar")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AparcaMalagaAppPreview() {
    AparcaMalagaApp(
        spots = listOf(
            ParkingSpot(
                id = "77785",
                address = "PASEO CERRADO DE CALDERÓN, 18",
                latitude = 36.72615397,
                longitude = -4.38156389,
                description = "Junto al número 18",
                spaceCount = 2,
            ),
        ),
        userLocation = UserLocation(36.72, -4.42),
    )
}

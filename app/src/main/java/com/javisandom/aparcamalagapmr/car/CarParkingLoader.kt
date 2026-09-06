package com.javisandom.aparcamalagapmr.car

import com.javisandom.aparcamalagapmr.domain.ParkingSpot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CarParkingState {
    data object Loading : CarParkingState
    data class Ready(val spots: List<ParkingSpot>) : CarParkingState
    data object Error : CarParkingState
}

class CarParkingLoader(private val read: suspend () -> List<ParkingSpot>) {
    private val mutableState = MutableStateFlow<CarParkingState>(CarParkingState.Loading)
    val state = mutableState.asStateFlow()
    suspend fun load() {
        mutableState.value = CarParkingState.Loading
        try {
            mutableState.value = CarParkingState.Ready(read())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = CarParkingState.Error
        }
    }
}

package com.javisandom.aparcamalagapmr.car

import androidx.car.app.CarContext
import com.javisandom.aparcamalagapmr.data.DeviceLocationSource
import com.javisandom.aparcamalagapmr.domain.UserLocation

object CarLocationProvider {
    fun lastKnown(carContext: CarContext): UserLocation? = DeviceLocationSource(carContext).latest()
}

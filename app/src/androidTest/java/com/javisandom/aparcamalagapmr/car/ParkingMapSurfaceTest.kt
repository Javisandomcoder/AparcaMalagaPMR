package com.javisandom.aparcamalagapmr.car

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import com.javisandom.aparcamalagapmr.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ParkingMapSurfaceTest {
    private val spot = ParkingSpot("77785", "PASEO CERRADO DE CALDERÓN, 18", 36.72615397, -4.38156389)

    @Test fun marker_can_be_selected_without_network_and_outside_taps_are_ignored() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val model = MovingParkingMap(listOf(spot))
        var selected: ParkingSpot? = null
        val renderer = ParkingMapSurface(InstrumentationRegistry.getInstrumentation().targetContext,
            model, scope, tileProvider = { _, _, _ -> null }) { selected = it }
        try {
            val bitmap = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
            renderer.draw(Canvas(bitmap))
            renderer.onClick(10f, 10f)
            assertNull(selected)
            renderer.onClick(400f, 240f)
            assertEquals(spot, selected)
            assertEquals(spot, model.selected)
        } finally { renderer.close(); scope.cancel() }
    }

    @Test fun host_overlay_moves_marker_into_the_unobstructed_area() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        var selected: ParkingSpot? = null
        val renderer = ParkingMapSurface(InstrumentationRegistry.getInstrumentation().targetContext,
            MovingParkingMap(listOf(spot)), scope, tileProvider = { _, _, _ -> null }) { selected = it }
        try {
            renderer.onVisibleAreaChanged(Rect(400, 0, 800, 400))
            renderer.draw(Canvas(Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)))
            renderer.onClick(400f, 240f)
            assertNull(selected)
            renderer.onClick(600f, 200f)
            assertEquals(spot, selected)
        } finally { renderer.close(); scope.cancel() }
    }

    @Test fun dark_mode_changes_the_map_background() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        var dark = false
        val renderer = ParkingMapSurface(InstrumentationRegistry.getInstrumentation().targetContext,
            MovingParkingMap(listOf(spot)), scope, isDarkMode = { dark }, tileProvider = { _, _, _ -> null }) {}
        try {
            val bitmap = Bitmap.createBitmap(800, 480, Bitmap.Config.ARGB_8888)
            renderer.draw(Canvas(bitmap))
            val lightPixel = bitmap.getPixel(799, 300)
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "map-qa-light.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            dark = true
            renderer.draw(Canvas(bitmap))
            assertNotEquals(lightPixel, bitmap.getPixel(799, 300))
            java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "map-qa-dark.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { renderer.close(); scope.cancel() }
    }
}

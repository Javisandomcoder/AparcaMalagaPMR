package com.javisandom.aparcamalagapmr.car

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.Surface
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import com.javisandom.aparcamalagapmr.domain.*
import kotlinx.coroutines.*
import kotlin.math.*

/** Map drawing does not invalidate templates or consume navigation steps. */
class ParkingMapSurface(
    private val context: Context,
    val model: MovingParkingMap,
    parentScope: CoroutineScope,
    private val isDarkMode: () -> Boolean = { false },
    private val tileProvider: ((Int, Int, Int) -> Bitmap?)? = null,
    private val select: (ParkingSpot) -> Unit,
) : SurfaceCallback {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val tiles = MapTileStore(context, scope, ::render)
    private var surface: Surface? = null
    private var width = 0
    private var height = 0
    private var area = Rect()
    private var density = 1f
    private var zoom = 16
    private var camera = model.center
    private var cameraHeading = if (model.following) model.heading.degrees else 0.0
    private var animationTarget: UserLocation? = null
    private var animationHeading: Double? = null
    private var animator: ValueAnimator? = null
    private val hits = mutableListOf<Pair<PointF, ParkingSpot>>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    var locationStatus: String = "Buscando ubicación"

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        android.util.Log.d("ParkingSurface", "available ${surfaceContainer.width}x${surfaceContainer.height} valid=${surfaceContainer.surface?.isValid}")
        if (surface !== surfaceContainer.surface) surface?.release()
        surface = surfaceContainer.surface
        width = surfaceContainer.width
        height = surfaceContainer.height
        density = (surfaceContainer.dpi / 160f).coerceIn(1f, 2f)
        render()
    }
    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        android.util.Log.d("ParkingSurface", "destroyed")
        surface?.release()
        surface = null
        animator?.cancel()
    }
    override fun onVisibleAreaChanged(visibleArea: Rect) {
        area = Rect(visibleArea)
        render()
    }
    override fun onStableAreaChanged(stableArea: Rect) {
        if (area.isEmpty) area = Rect(stableArea)
        render()
    }
    fun close() {
        animator?.cancel()
        job.cancel()
        surface?.release()
        surface = null
    }
    fun update(location: UserLocation?, status: String) {
        locationStatus = status
        model.updateLocation(location)
        moveCamera()
    }
    fun recenter() { model.recenter(); moveCamera() }
    private fun moveCamera() {
        val desired = model.center
        val desiredHeading = if (model.following) model.heading.degrees else cameraHeading
        val previousTarget = animationTarget
        if (previousTarget != null && animationHeading != null &&
            distanceMeters(previousTarget, ParkingSpot("camera", "", desired.latitude, desired.longitude)) < 5 &&
            abs(shortestHeadingDelta(animationHeading!!, desiredHeading)) < 2) {
            render()
            return
        }
        animationTarget = desired
        animationHeading = desiredHeading
        animator?.cancel()
        val from = camera
        val fromHeading = cameraHeading
        val targetHeading = desiredHeading
        val headingDelta = shortestHeadingDelta(fromHeading, targetHeading)
        val target = model.center
        if (from == target && abs(headingDelta) < 0.1) { render(); return }
        if (distanceMeters(from, ParkingSpot("camera", "", target.latitude, target.longitude)) > 2_000) {
            camera = target
            cameraHeading = targetHeading
            render()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = max(1_400L, (abs(headingDelta) / 30.0 * 1_000).toLong())
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener {
                val fraction = it.animatedValue as Float
                cameraHeading = fromHeading + headingDelta * fraction
                camera = UserLocation(from.latitude + (target.latitude - from.latitude) * fraction,
                    from.longitude + (target.longitude - from.longitude) * fraction)
                render()
            }
            start()
        }
    }
    override fun onScroll(distanceX: Float, distanceY: Float) {
        animator?.cancel()
        val point = MapProjection.project(camera, zoom)
        camera = MapProjection.unproject(MapPoint(point.x + distanceX * cos(Math.toRadians(cameraHeading)) - distanceY * sin(Math.toRadians(cameraHeading)),
            point.y + distanceX * sin(Math.toRadians(cameraHeading)) + distanceY * cos(Math.toRadians(cameraHeading))), zoom)
        model.explore(camera)
        animationTarget = null
        animationHeading = null
        render()
    }
    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        if (scaleFactor > 1.15) changeZoom(1)
        else if (scaleFactor < 0.85) changeZoom(-1)
    }
    fun changeZoom(delta: Int) { zoom = (zoom + delta).coerceIn(13, 18); render() }
    override fun onClick(x: Float, y: Float) {
        val hit = hits.minByOrNull { (point, _) -> hypot(point.x - x, point.y - y) } ?: return
        if (hypot(hit.first.x - x, hit.first.y - y) <= 32 * density) {
            model.select(hit.second)
            select(hit.second)
        }
    }

    fun render() {
        val target = surface ?: return
        if (!target.isValid || width <= 0 || height <= 0) return
        val canvas = try { target.lockCanvas(null) } catch (_: IllegalArgumentException) { return }
            catch (_: Surface.OutOfResourcesException) { return }
        try { draw(canvas) } finally { target.unlockCanvasAndPost(canvas) }
    }

    internal fun draw(canvas: Canvas) {
        val width = canvas.width
        val height = canvas.height
        val dark = isDarkMode()
        canvas.drawColor(Color.parseColor(if (dark) "#17212A" else "#EDF1F0"))
        val viewport = if (area.isEmpty) Rect(0, 0, width, height) else area
        val projected = MapProjection.project(camera, zoom)
        val anchorX = viewport.exactCenterX()
        val anchorY = if (model.following && model.origin != null) viewport.top + viewport.height() * .65f else viewport.exactCenterY()
        val offsetX = anchorX - projected.x
        val offsetY = anchorY - projected.y
        fun screenPoint(location: UserLocation): PointF {
            val point = MapProjection.project(location, zoom)
            val dx = point.x - projected.x
            val dy = point.y - projected.y
            val angle = Math.toRadians(-cameraHeading)
            return PointF((anchorX + dx * cos(angle) - dy * sin(angle)).toFloat(),
                (anchorY + dx * sin(angle) + dy * cos(angle)).toFloat())
        }
        // Paint only the geographic map on Surface; controls belong to host templates.
        paint.colorFilter = if (dark) ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
            -.65f,0f,0f,0f,185f, 0f,-.65f,0f,0f,190f, 0f,0f,-.65f,0f,200f, 0f,0f,0f,1f,0f))) else null
        var loaded = 0
        canvas.save()
        canvas.rotate(-cameraHeading.toFloat(), anchorX, anchorY)
        val angle = Math.toRadians(cameraHeading)
        val corners = listOf(0.0 to 0.0, width.toDouble() to 0.0,
            0.0 to height.toDouble(), width.toDouble() to height.toDouble()).map { (x, y) ->
            val dx = x - anchorX; val dy = y - anchorY
            MapPoint(projected.x + dx * cos(angle) - dy * sin(angle),
                projected.y + dx * sin(angle) + dy * cos(angle))
        }
        for (x in floor(corners.minOf { it.x } / 256).toInt()..floor(corners.maxOf { it.x } / 256).toInt()) {
            for (y in floor(corners.minOf { it.y } / 256).toInt()..floor(corners.maxOf { it.y } / 256).toInt()) {
                val bitmap = (if (tileProvider != null) tileProvider.invoke(zoom, x, y) else tiles.tile(zoom, x, y)) ?: continue
                loaded++
                val left = (x * 256 + offsetX).toFloat()
                val top = (y * 256 + offsetY).toFloat()
                canvas.drawBitmap(bitmap, null, RectF(left, top, left + 256, top + 256), paint)
            }
        }
        canvas.restore()
        paint.colorFilter = null
        hits.clear()
        val radius = 20 * density
        // Suppress overlapping markers, retaining nearest candidates first.
        for (spot in model.markers) {
            val point = screenPoint(UserLocation(spot.latitude, spot.longitude))
            val x = point.x
            val y = point.y
            if (x < viewport.left + radius || x > viewport.right - radius ||
                y < viewport.top + 65 * density || y > viewport.bottom - 36 * density) continue
            if (hits.any { hypot(it.first.x - x, it.first.y - y) < radius * 2.3 }) continue
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, radius + 3 * density, paint)
            paint.color = Color.parseColor("#176B92")
            canvas.drawCircle(x, y, radius, paint)
            paint.color = Color.WHITE
            paint.textSize = 25 * density
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("P", x, y + 8 * density, paint)
            hits.add(PointF(x, y) to spot)
        }
        model.origin?.let {
            // Keep the vehicle anchored while the geography moves underneath it.
            val point = if (model.following) PointF(anchorX, anchorY) else screenPoint(it)
            val x = point.x
            val y = point.y
            paint.color = Color.parseColor("#332A83EC")
            canvas.drawCircle(x, y, 23 * density, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(x, y, 11 * density, paint)
            paint.color = Color.parseColor("#2378DB")
            canvas.save()
            canvas.rotate(if (model.following) 0f else (model.heading.degrees - cameraHeading).toFloat(), x, y)
            val arrow = Path().apply {
                moveTo(x, y - 15 * density); lineTo(x + 10 * density, y + 11 * density)
                lineTo(x, y + 6 * density); lineTo(x - 10 * density, y + 11 * density); close()
            }
            canvas.drawPath(arrow, paint)
            canvas.restore()
        }
        val status = if (model.following) locationStatus else "Explorando el mapa"
        label(canvas, status, viewport.left + 12f * density, viewport.top + 12f * density, dark)
        if (loaded == 0) label(canvas, "Cartografía no disponible", viewport.left + 12f * density,
            viewport.top + 50f * density, dark)
        label(canvas, "© OpenStreetMap contributors", viewport.left + 8f * density,
            viewport.bottom - 29f * density, dark, 12f)
    }
    private fun label(canvas: Canvas, text: String, x: Float, y: Float, dark: Boolean, size: Float = 16f) {
        paint.textSize = size * density
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT
        val length = paint.measureText(text)
        paint.color = Color.parseColor(if (dark) "#E6202C36" else "#F5FFFFFF")
        canvas.drawRoundRect(x, y, x + length + 16 * density, y + (size + 14) * density, 8f, 8f, paint)
        paint.color = Color.parseColor(if (dark) "#EDF5F9" else "#243C48")
        canvas.drawText(text, x + 8 * density, y + (size + 3) * density, paint)
    }
}

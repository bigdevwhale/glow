package app.glow.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import app.glow.ui.theme.U
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.random.Random

/** One win as a stone: [size] 1..3 sets how big it is. */
data class JarStone(val id: Long, val size: Int, val color: Color)

/**
 * A glass jar that fills with a stone per win. Stones drop in one by one, roll under gravity
 * from the accelerometer and jostle when the phone is shaken. Tapping a stone reports its id.
 */
@Composable
fun Jar(stones: List<JarStone>, modifier: Modifier = Modifier, live: Boolean = true, onTap: (Long) -> Unit = {}) {
    val sim = remember(stones) { JarSim(stones) }
    var frame by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current

    if (live) {
        DisposableEffect(sim) {
            val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    // Screen x grows to the right, y downward; the sensor reports the opposite of gravity on x.
                    sim.gx = (-e.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-2.5f, 2.5f)
                    sim.gy = (e.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-2.5f, 2.5f)
                }

                override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            }
            if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sm.unregisterListener(listener) }
        }
    }
    LaunchedEffect(sim) {
        while (true) {
            withFrameNanos { frame = it }
            sim.step()
        }
    }

    val glass = U.c.ink
    val lid = U.c.gold
    val glow = U.c.accent
    val fill = U.c.surface
    Canvas(
        modifier
            .onSizeChanged { sim.layout(it) }
            .pointerInput(sim) { detectTapGestures { p -> sim.hit(p)?.let(onTap) } },
    ) {
        frame.let { } // redraw every frame
        val g = sim.geometry ?: return@Canvas
        val body = g.jarPath()
        drawPath(body, fill.copy(alpha = 0.55f))
        val fullness = (sim.count.toFloat() / 40f).coerceIn(0f, 1f)
        if (sim.count > 0) {
            drawCircle(
                Brush.radialGradient(listOf(glow.copy(alpha = 0.10f + 0.18f * fullness), Color.Transparent), Offset(g.cx, g.bottom - g.h * 0.2f), g.w * 0.6f),
                g.w * 0.6f, Offset(g.cx, g.bottom - g.h * 0.2f),
            )
        }
        for (i in 0 until sim.count) {
            val p = Offset(sim.x[i], sim.y[i])
            val r = sim.r[i]
            val c = sim.colors[i]
            drawCircle(Color.Black.copy(alpha = 0.10f), r, p + Offset(0f, r * 0.12f))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f).compositeOver(c), c, c.darker()), p - Offset(r * 0.35f, r * 0.4f), r * 1.5f), r, p)
            drawCircle(Color.White.copy(alpha = 0.5f), r * 0.18f, p - Offset(r * 0.38f, r * 0.42f))
        }
        // Glass outline and a highlight down the left side.
        drawPath(body, glass.copy(alpha = 0.22f), style = Stroke(g.w * 0.012f))
        drawLine(Color.White.copy(alpha = 0.55f), Offset(g.left + g.w * 0.06f, g.shoulder + g.h * 0.08f), Offset(g.left + g.w * 0.06f, g.bottom - g.h * 0.18f), g.w * 0.02f, StrokeCap.Round)
        drawRoundRect(lid, Offset(g.neckLeft - g.w * 0.03f, g.top - g.h * 0.055f), Size(g.neckRight - g.neckLeft + g.w * 0.06f, g.h * 0.07f), CornerRadius(g.w * 0.03f))
        drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(g.neckLeft, g.top - g.h * 0.045f), Size(g.neckRight - g.neckLeft, g.h * 0.015f), CornerRadius(g.w * 0.01f))
    }
}

private fun Color.darker(): Color = Color(red * 0.78f, green * 0.78f, blue * 0.78f, alpha)

private fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(red * a + bg.red * (1 - a), green * a + bg.green * (1 - a), blue * a + bg.blue * (1 - a), 1f)
}

private class Geometry(val w: Float, val h: Float) {
    val left = w * 0.08f
    val right = w * 0.92f
    val top = h * 0.07f
    val shoulder = h * 0.2f
    val bottom = h * 0.98f
    val neckLeft = w * 0.27f
    val neckRight = w * 0.73f
    val corner = w * 0.16f
    val cx = w / 2

    fun jarPath() = Path().apply {
        moveTo(neckLeft, top)
        lineTo(neckLeft, top + h * 0.04f)
        cubicTo(neckLeft, shoulder - h * 0.04f, left, shoulder - h * 0.06f, left, shoulder + h * 0.04f)
        lineTo(left, bottom - corner)
        quadraticTo(left, bottom, left + corner, bottom)
        lineTo(right - corner, bottom)
        quadraticTo(right, bottom, right, bottom - corner)
        lineTo(right, shoulder + h * 0.04f)
        cubicTo(right, shoulder - h * 0.06f, neckRight, shoulder - h * 0.04f, neckRight, top + h * 0.04f)
        lineTo(neckRight, top)
        close()
    }
}

/** Verlet circles in a box with rounded bottom corners. */
private class JarSim(private val stones: List<JarStone>) {
    val x = FloatArray(stones.size)
    val y = FloatArray(stones.size)
    private val px = FloatArray(stones.size)
    private val py = FloatArray(stones.size)
    val r = FloatArray(stones.size)
    val colors = stones.map { it.color }
    var count = 0
        private set
    var geometry: Geometry? = null
        private set
    @Volatile var gx = 0f
    @Volatile var gy = 1f
    private var tick = 0
    private val rnd = Random(stones.size)

    fun layout(size: IntSize) {
        if (size.width == 0 || geometry?.w == size.width.toFloat()) return
        val g = Geometry(size.width.toFloat(), size.height.toFloat())
        geometry = g
        val unit = g.w * 0.05f
        val base = stones.map { unit * (0.6f + 0.3f * it.size) }
        // Shrink everything when a busy month would overflow the jar.
        val area = (g.right - g.left) * (g.bottom - g.shoulder) * 0.62f
        val need = base.sumOf { (PI * it * it).toDouble() }.toFloat()
        val scale = if (need > area) sqrt(area / need) else 1f
        base.forEachIndexed { i, v -> r[i] = v * scale }
        count = 0
    }

    fun hit(p: Offset): Long? {
        var best: Int? = null
        var bestD = Float.MAX_VALUE
        for (i in 0 until count) {
            val dx = x[i] - p.x
            val dy = y[i] - p.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < r[i] * 1.4f && d < bestD) { best = i; bestD = d }
        }
        return best?.let { stones[it].id }
    }

    fun step() {
        val g = geometry ?: return
        tick++
        // Drop the next stone through the neck every couple of frames.
        if (count < stones.size && tick % (if (stones.size > 60) 1 else 3) == 0) {
            val i = count
            x[i] = g.cx + (rnd.nextFloat() - 0.5f) * (g.neckRight - g.neckLeft) * 0.5f
            y[i] = g.top + r[i]
            px[i] = x[i]
            py[i] = y[i] - 2f
            count++
        }
        val dt = 1f / 60f
        val accel = g.h * 2.2f
        // Lying flat on a table, the screen feels no gravity; keep a gentle pull toward the bottom then.
        val tilt = sqrt(gx * gx + gy * gy).coerceAtMost(1f)
        val ax = gx * accel * dt * dt
        val ay = (gy + 0.8f * (1f - tilt)) * accel * dt * dt
        for (i in 0 until count) {
            val vx = (x[i] - px[i]) * 0.985f
            val vy = (y[i] - py[i]) * 0.985f
            px[i] = x[i]; py[i] = y[i]
            x[i] += vx + ax
            y[i] += vy + ay
        }
        repeat(4) {
            for (i in 0 until count) {
                for (j in i + 1 until count) {
                    val dx = x[j] - x[i]
                    val dy = y[j] - y[i]
                    val min = r[i] + r[j]
                    val d2 = dx * dx + dy * dy
                    if (d2 < min * min && d2 > 0.0001f) {
                        val d = sqrt(d2)
                        val push = (min - d) / d * 0.5f
                        x[i] -= dx * push; y[i] -= dy * push
                        x[j] += dx * push; y[j] += dy * push
                    }
                }
                walls(i, g)
            }
        }
    }

    private fun walls(i: Int, g: Geometry) {
        val ri = r[i]
        val inset = g.w * 0.012f
        val l = g.left + inset + ri
        val rr = g.right - inset - ri
        val b = g.bottom - inset - ri
        val t = g.top + ri
        x[i] = x[i].coerceIn(l, rr)
        y[i] = y[i].coerceIn(t, b)
        // Above the shoulders only the neck is open.
        if (y[i] < g.shoulder) x[i] = x[i].coerceIn(g.neckLeft + ri * 0.6f, g.neckRight - ri * 0.6f)
        // Rounded bottom corners.
        val c = g.corner
        val cy = g.bottom - c
        listOf(g.left + c, g.right - c).forEachIndexed { k, cxc ->
            val inside = if (k == 0) x[i] < cxc else x[i] > cxc
            if (inside && y[i] > cy) {
                val dx = x[i] - cxc
                val dy = y[i] - cy
                val d = sqrt(dx * dx + dy * dy)
                val max = c - inset - ri
                if (d > max && d > 0f) { x[i] = cxc + dx / d * max; y[i] = cy + dy / d * max }
            }
        }
    }
}

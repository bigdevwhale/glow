package app.glow.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.glow.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The brand mark: a soft orb of light that breathes. [strength] 0..1 makes it brighter and larger. */
@Composable
fun GlowOrb(modifier: Modifier = Modifier, strength: Float = 0.6f, color: Color = U.c.accent, warm: Color = U.c.gold, content: @Composable () -> Unit = {}) {
    val t = rememberInfiniteTransition(label = "orb")
    val pulse by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse), label = "pulse")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "spin")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2
            val s = 0.55f + 0.45f * strength
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.10f + 0.12f * pulse * s), Color.Transparent), c, r), r, c)
            drawCircle(Brush.radialGradient(listOf(warm.copy(alpha = 0.35f * s), color.copy(alpha = 0.18f * s), Color.Transparent), c, r * (0.7f + 0.06f * pulse)), r * (0.7f + 0.06f * pulse), c)
            // Rays, slowly turning.
            rotate(spin, c) {
                repeat(12) { i ->
                    val a = i * 30.0 * PI / 180
                    val r0 = r * 0.42f
                    val r1 = r * (0.62f + 0.06f * ((i % 3) / 2f) + 0.04f * pulse)
                    drawLine(
                        warm.copy(alpha = 0.22f * s),
                        Offset(c.x + r0 * cos(a).toFloat(), c.y + r0 * sin(a).toFloat()),
                        Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                        strokeWidth = r * 0.025f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
            val core = r * (0.36f + 0.03f * pulse)
            drawCircle(Brush.radialGradient(listOf(Color.White, warm, color), Offset(c.x - core * 0.3f, c.y - core * 0.35f), core * 1.5f), core, c)
        }
        content()
    }
}

/** A burst of falling confetti; plays once for [key]. [amount] scales the number of pieces. */
@Composable
fun Confetti(key: Any, modifier: Modifier = Modifier, amount: Float = 1f) {
    val colors = listOf(U.c.accent, U.c.gold, U.c.sage, U.c.sky, U.c.rose, U.c.violet)
    val pieces = remember(key) {
        val rnd = Random(key.hashCode())
        List((90 * amount).toInt().coerceAtLeast(10)) {
            Piece(rnd.nextFloat(), -rnd.nextFloat() * 0.4f, 0.25f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 0.3f, colors[it % colors.size])
        }
    }
    var elapsed by remember(key) { mutableLongStateOf(0L) }
    LaunchedEffect(key) {
        val start = withFrameMillis { it }
        while (elapsed < 3200) {
            elapsed = withFrameMillis { it } - start
        }
    }
    if (elapsed >= 3200) return
    Canvas(modifier) {
        val t = elapsed / 1000f
        val fade = (1f - (t - 2.2f).coerceAtLeast(0f)).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val y = (p.y + p.speed * t * t * 0.6f + p.speed * t * 0.4f) * size.height
            val x = (p.x + p.drift * t + 0.02f * sin(t * 4 + p.rot)) * size.width
            rotate(p.rot + t * 220f * p.drift * 6, Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - 5f, y - 9f), Size(10f, 18f))
            }
        }
    }
}

private class Piece(val x: Float, val y: Float, val speed: Float, val rot: Float, val drift: Float, val color: Color)

/** Sparkles and hearts drifting upward forever; the backdrop of the SOS screen. */
@Composable
fun RisingSparkles(modifier: Modifier = Modifier, colors: List<Color>) {
    val motes = remember {
        val rnd = Random(7)
        List(46) { Mote(rnd.nextFloat(), rnd.nextFloat(), 0.04f + rnd.nextFloat() * 0.08f, 2f + rnd.nextFloat() * 5f, rnd.nextFloat() * 6.28f, it % 5 == 0) }
    }
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) time = withFrameMillis { it } - start
    }
    Canvas(modifier) {
        val t = time / 1000f
        motes.forEachIndexed { i, m ->
            val y = 1.1f - ((m.y + m.speed * t) % 1.2f)
            val x = m.x + 0.03f * sin(t * 0.8f + m.phase)
            val p = Offset(x * size.width, y * size.height)
            val tw = 0.5f + 0.5f * sin(t * 2.2f + m.phase)
            val c = colors[i % colors.size].copy(alpha = 0.25f + 0.55f * tw)
            val r = m.size.dp.toPx()
            if (m.heart) heart(p, r * 1.6f, c) else sparkle(p, r, c)
        }
    }
}

private class Mote(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float, val heart: Boolean)

private fun DrawScope.sparkle(p: Offset, r: Float, c: Color) {
    val path = Path().apply {
        moveTo(p.x, p.y - r * 2)
        quadraticTo(p.x, p.y, p.x + r * 2, p.y)
        quadraticTo(p.x, p.y, p.x, p.y + r * 2)
        quadraticTo(p.x, p.y, p.x - r * 2, p.y)
        quadraticTo(p.x, p.y, p.x, p.y - r * 2)
        close()
    }
    drawPath(path, c)
    drawCircle(c.copy(alpha = c.alpha * 0.3f), r * 1.4f, p)
}

private fun DrawScope.heart(p: Offset, r: Float, c: Color) {
    val path = Path().apply {
        moveTo(p.x, p.y + r * 0.9f)
        cubicTo(p.x - r * 1.4f, p.y - r * 0.1f, p.x - r * 0.8f, p.y - r * 1.2f, p.x, p.y - r * 0.45f)
        cubicTo(p.x + r * 0.8f, p.y - r * 1.2f, p.x + r * 1.4f, p.y - r * 0.1f, p.x, p.y + r * 0.9f)
        close()
    }
    drawPath(path, c)
}

/** Colours of an order's degrees: 1 bronze, 2 silver, 3 gold; 0 is not earned yet. */
fun medalColors(degree: Int): Pair<Color, Color> = when (degree) {
    3 -> Color(0xFFF5CF5B) to Color(0xFFC8901A)
    2 -> Color(0xFFE3E7EE) to Color(0xFF8F98A6)
    1 -> Color(0xFFE6A26B) to Color(0xFFA35E2C)
    else -> Color(0x33888888) to Color(0x55888888)
}

/** A round medal on a two-tailed ribbon. Put the order's icon on top as [content]. */
@Composable
fun Medal(degree: Int, ribbon: Color, modifier: Modifier = Modifier, size: Dp = 64.dp, content: @Composable () -> Unit = {}) {
    val (light, deep) = medalColors(degree)
    val earned = degree > 0
    val faint = U.c.line
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val c = Offset(w / 2, h * 0.58f)
            val r = minOf(w, h) * 0.36f
            // Ribbon tails behind the disc.
            val tail = if (earned) ribbon else faint
            listOf(-1f, 1f).forEach { side ->
                val path = Path().apply {
                    moveTo(c.x + side * r * 0.15f, c.y - r * 1.5f)
                    lineTo(c.x + side * r * 0.85f, c.y - r * 1.5f)
                    lineTo(c.x + side * r * 0.55f, c.y - r * 0.2f)
                    lineTo(c.x + side * r * 0.05f, c.y - r * 0.4f)
                    close()
                }
                drawPath(path, tail.copy(alpha = if (earned) 1f else 0.6f))
                drawPath(path, Color.Black.copy(alpha = 0.08f), style = Stroke(1f))
            }
            if (earned) drawCircle(Brush.radialGradient(listOf(light.copy(alpha = 0.5f), Color.Transparent), c, r * 1.5f), r * 1.5f, c)
            drawCircle(Brush.linearGradient(listOf(light, deep), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)), r, c)
            drawCircle(Color.White.copy(alpha = if (earned) 0.45f else 0.15f), r * 0.82f, c, style = Stroke(r * 0.06f))
            if (earned) drawCircle(Color.White.copy(alpha = 0.35f), r * 0.18f, Offset(c.x - r * 0.45f, c.y - r * 0.45f))
        }
        // The disc sits a little below the middle, under the ribbon.
        Box(Modifier.fillMaxSize().padding(top = size * 0.16f), contentAlignment = Alignment.Center) { content() }
    }
}

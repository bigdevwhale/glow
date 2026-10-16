package app.glow.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.ui.GlowData
import app.glow.ui.Haptics
import app.glow.ui.praise
import app.glow.ui.voiceText
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.RisingSparkles
import app.glow.ui.theme.Inter
import app.glow.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Night = Color(0xFF1B1030)
private val Plum = Color(0xFF3B1640)
private val Pink = Color(0xFFFF7AA8)
private val Honey = Color(0xFFFFC36B)
private val Lavender = Color(0xFFB9A6FF)

/**
 * For a bad moment: a dark, soft room with drifting sparkles, the voice's comforting lines one by one,
 * and an optional breathing circle (4 in, 2 hold, 6 out). Always dark, whatever the theme.
 */
@Composable
fun SosScreen(data: GlowData, onClose: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val s = data.settings
    val lines = voiceText(s.voice).sos
    var index by rememberSaveable { mutableIntStateOf((data.now / 60_000 % lines.size).toInt()) }
    var breathing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(breathing) {
        if (breathing) return@LaunchedEffect
        while (true) {
            delay(6500)
            index = (index + 1) % lines.size
            if (s.haptics) Haptics.heartbeat(context)
        }
    }

    // Light status-bar icons on the dark room, put back on the way out.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        val wasLight = bars?.isAppearanceLightStatusBars
        bars?.isAppearanceLightStatusBars = false
        bars?.isAppearanceLightNavigationBars = false
        onDispose {
            if (wasLight != null) {
                bars.isAppearanceLightStatusBars = wasLight
                bars.isAppearanceLightNavigationBars = wasLight
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Night, Plum, Night)))) {
        RisingSparkles(Modifier.fillMaxSize(), listOf(Pink, Honey, Lavender))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White.copy(alpha = 0.7f)) }
            }
            Spacer(Modifier.weight(0.6f))
            if (breathing) Breather() else {
                GlowOrb(Modifier.size(200.dp), strength = 0.5f, color = Pink, warm = Honey) { Text(s.voice.emoji, fontSize = 40.sp) }
                Spacer(Modifier.height(28.dp))
                AnimatedContent(index, transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(600)) }, label = "sos") { i ->
                    Text(
                        praise(lines[i % lines.size]),
                        style = UType.Display.copy(fontSize = 26.sp, lineHeight = 34.sp, color = Color.White, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().clickable { index = (index + 1) % lines.size },
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.sos_help), style = UType.Small.copy(color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center))
            Spacer(Modifier.height(14.dp))
            Pill(if (breathing) "✕  " + stringResource(R.string.sos_breathe) else stringResource(R.string.sos_breathe), Color.White.copy(alpha = 0.12f), Color.White) { breathing = !breathing }
            Spacer(Modifier.height(10.dp))
            Pill(stringResource(R.string.sos_better), Pink, Night) {
                container.scope.launch { container.settings.update { it.copy(sosCount = it.sosCount + 1) } }
                onClose()
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontFamily = Inter, fontSize = 15.sp, color = fg, style = UType.Button)
    }
}

/** A circle that grows on the in-breath and shrinks on the out-breath, with the phase written inside. */
@Composable
private fun Breather() {
    val scale = remember { Animatable(0.55f) }
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            phase = 0
            scale.animateTo(1f, tween(4000, easing = FastOutSlowInEasing))
            phase = 1
            delay(2000)
            phase = 2
            scale.animateTo(0.55f, tween(6000, easing = LinearEasing))
        }
    }
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(260.dp).scale(scale.value).clip(CircleShape).background(Brush.radialGradient(listOf(Honey.copy(alpha = 0.55f), Pink.copy(alpha = 0.35f), Color.Transparent))))
        Box(Modifier.size(150.dp).scale(scale.value).clip(CircleShape).background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.9f), Honey, Pink))))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Air, null, Modifier.size(22.dp), tint = Night.copy(alpha = 0.6f))
            Text(
                stringResource(when (phase) { 0 -> R.string.sos_in; 1 -> R.string.sos_hold; else -> R.string.sos_out }),
                style = UType.Strong.copy(color = Night, fontSize = 18.sp),
            )
        }
    }
}

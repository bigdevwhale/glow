package app.glow.ui.kind

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.ui.GlowData
import app.glow.ui.components.GlowOrb
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import java.io.File

/** Kind words one at a time, in random order; a tap brings the next. */
@Composable
fun ShuffleScreen(data: GlowData, onClose: () -> Unit) {
    val container = LocalContext.current.container
    val order = remember(data.kindWords.size) { data.kindWords.shuffled() }
    var index by rememberSaveable { mutableIntStateOf(0) }
    if (order.isEmpty()) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onClose() }
        return
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(U.c.goldSoft, U.c.bg, U.c.accentSoft)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { index++ },
    ) {
        GlowOrb(Modifier.fillMaxWidth().height(320.dp).align(Alignment.Center), strength = 0.3f)
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = U.c.muted) }
            }
            AnimatedContent(
                index,
                Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { (fadeIn(tween(500)) + scaleIn(initialScale = 0.94f)) togetherWith fadeOut(tween(250)) },
                label = "kind",
            ) { i ->
                val k = order[i % order.size]
                Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    val image = rememberImage(k.image?.let { File(container.images, it) })
                    if (image != null) {
                        Image(image, null, Modifier.fillMaxWidth().heightIn(max = 340.dp).clip(U.RLg), contentScale = ContentScale.Fit)
                        Spacer(Modifier.height(18.dp))
                    }
                    if (k.text.isNotBlank()) {
                        Text(k.text, style = UType.Display.copy(fontSize = if (k.text.length > 140) 21.sp else 26.sp, lineHeight = if (k.text.length > 140) 29.sp else 34.sp, textAlign = TextAlign.Center))
                    }
                    val from = kindFrom(k)
                    if (from.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.kind_from, from), style = UType.Strong.copy(color = U.c.accent))
                    }
                }
            }
            Text(stringResource(R.string.kind_tap_next), style = UType.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp))
        }
    }
}

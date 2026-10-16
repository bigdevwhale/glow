package app.glow.ui.today

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.data.KindWord
import app.glow.domain.Voice
import app.glow.domain.WinSize
import app.glow.ui.GlowData
import app.glow.ui.Go
import app.glow.ui.Haptics
import app.glow.ui.voiceText
import app.glow.ui.components.Confetti
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.SoftButton
import app.glow.ui.theme.U
import app.glow.ui.theme.UType

/** The moment after saving a win: the voice says its piece, with confetti as big as the win. */
@Composable
fun PraiseScreen(id: Long, data: GlowData, go: Go) {
    val context = LocalContext.current
    val container = context.container
    val win = data.wins.firstOrNull { it.id == id } ?: return
    val size = WinSize.of(win.size)
    val voice = Voice.of(win.voice)
    val saved = data.kindWords.any { it.source == KindWord.SOURCE_GLOW && it.text == win.praise }
    val savedToast = stringResource(R.string.saved_to_kind)

    val played = rememberSaveable { booleanArrayOf(false) }
    val pop = remember { Animatable(if (played[0]) 1f else 0f) }
    val textIn = remember { Animatable(if (played[0]) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (played[0]) return@LaunchedEffect
        played[0] = true
        if (data.settings.haptics) Haptics.praise(context, size)
        pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        textIn.animateTo(1f, tween(500))
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(U.c.accentSoft, U.c.bg, U.c.goldSoft))),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            val orb = when (size) { WinSize.TINY -> 170.dp; WinSize.SOLID -> 210.dp; WinSize.HUGE -> 250.dp }
            GlowOrb(Modifier.size(orb).scale(0.4f + 0.6f * pop.value), strength = size.value / 3f) {
                Text(voice.emoji, fontSize = (34 + 8 * size.value).sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(when (size) { WinSize.TINY -> R.string.win_tiny; WinSize.SOLID -> R.string.win_solid; WinSize.HUGE -> R.string.win_huge }).uppercase(),
                style = UType.Eyebrow.copy(color = U.c.accent, fontSize = 12.sp),
            )
            Spacer(Modifier.height(6.dp))
            Text("«" + win.text + "»", style = UType.Sub.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(22.dp))
            Text(
                win.praise,
                style = UType.Display.copy(
                    fontSize = when (size) { WinSize.TINY -> 26.sp; WinSize.SOLID -> 29.sp; WinSize.HUGE -> 31.sp },
                    lineHeight = when (size) { WinSize.TINY -> 33.sp; WinSize.SOLID -> 36.sp; WinSize.HUGE -> 39.sp },
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.alpha(textIn.value),
            )
            Spacer(Modifier.height(10.dp))
            Text("— " + voiceText(voice).name, style = UType.Small, modifier = Modifier.alpha(textIn.value))
            Spacer(Modifier.height(24.dp))
        }
            PrimaryButton(stringResource(R.string.thanks), { go.home() }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.another_win), { go.home(); go.addWin() }, Modifier.weight(1f), icon = Icons.Rounded.Add)
                SoftButton(
                    stringResource(R.string.keep), {
                        if (!saved) {
                            container.write { dao().insertKindWord(KindWord(at = System.currentTimeMillis(), text = win.praise, from = voice.name, source = KindWord.SOURCE_GLOW)) }
                            Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                        }
                    },
                    Modifier.weight(1f), icon = if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Confetti(id, Modifier.fillMaxSize(), amount = when (size) { WinSize.TINY -> 0.25f; WinSize.SOLID -> 0.8f; WinSize.HUGE -> 1.8f })
    }
}

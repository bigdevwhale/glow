package app.glow.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.data.Evening
import app.glow.domain.WinSize
import app.glow.domain.pick
import app.glow.ui.GlowData
import app.glow.ui.Haptics
import app.glow.ui.gs
import app.glow.ui.praise
import app.glow.ui.voiceText
import app.glow.ui.components.Confetti
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.TopBar
import app.glow.ui.components.UTextField
import app.glow.ui.theme.U
import app.glow.ui.theme.UType

/** Three things the user is proud of today; saving them ends with a goodnight from the voice. */
@Composable
fun EveningScreen(data: GlowData, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val existing = data.stats.todayEvening
    var one by rememberSaveable { mutableStateOf(existing?.one.orEmpty()) }
    var two by rememberSaveable { mutableStateOf(existing?.two.orEmpty()) }
    var three by rememberSaveable { mutableStateOf(existing?.three.orEmpty()) }
    var done by rememberSaveable { mutableStateOf(false) }
    val s = data.settings

    AnimatedContent(done, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "evening") { finished ->
        if (finished) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(U.c.violetSoft, U.c.bg)))) {
                Column(
                    Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(1f))
                    GlowOrb(Modifier.size(180.dp), strength = 0.5f, color = U.c.violet) { Text("🌙", fontSize = 44.sp) }
                    Spacer(Modifier.height(18.dp))
                    val line = voiceText(s.voice).evening.pick(data.stats.today.toEpochDay())
                    Text(praise(line), style = UType.Display.copy(fontSize = 26.sp, lineHeight = 33.sp, textAlign = TextAlign.Center))
                    Spacer(Modifier.height(8.dp))
                    Text("— " + voiceText(s.voice).name, style = UType.Small)
                    Spacer(Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.thanks), onBack, Modifier.fillMaxWidth(), color = U.c.violet)
                }
                Confetti("evening", Modifier.fillMaxSize(), amount = 0.4f)
            }
        } else {
            Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
                TopBar("", onBack)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
                    Text(stringResource(R.string.evening_title), style = UType.Title)
                    Spacer(Modifier.height(6.dp))
                    Text(gs(R.string.evening_intro), style = UType.Sub)
                    Spacer(Modifier.height(20.dp))
                    Field(1, one, { one = it }, gs(R.string.evening_1))
                    Field(2, two, { two = it }, gs(R.string.evening_2))
                    Field(3, three, { three = it }, gs(R.string.evening_3))
                }
                Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding()) {
                    PrimaryButton(
                        stringResource(R.string.evening_save),
                        {
                            val day = data.stats.today.toEpochDay()
                            container.write { dao().setEvening(Evening(day, one.trim(), two.trim(), three.trim(), System.currentTimeMillis())) }
                            if (s.haptics) Haptics.praise(context, WinSize.SOLID)
                            done = true
                        },
                        Modifier.fillMaxWidth(),
                        enabled = listOf(one, two, three).any { it.isNotBlank() },
                        color = U.c.violet,
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(n: Int, value: String, onChange: (String) -> Unit, hint: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 12.dp)) {
        Text("$n.", style = UType.Number.copy(color = U.c.violet), modifier = Modifier.width(30.dp).padding(top = 12.dp))
        UTextField(value, { onChange(it.take(200)) }, placeholder = hint, minLines = 2)
    }
}

package app.glow.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.domain.Voice
import app.glow.domain.pick
import app.glow.ui.GlowData
import app.glow.ui.praise
import app.glow.ui.voiceText
import app.glow.ui.components.TopBar
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.launch

@Composable
fun VoiceScreen(data: GlowData, onBack: () -> Unit) {
    val container = LocalContext.current.container
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.voice), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Text(stringResource(R.string.voice_sub), style = UType.Sub)
            Spacer(Modifier.height(14.dp))
            VoiceList(data.settings.voice, data.stats.today.toEpochDay()) { v ->
                container.scope.launch {
                    container.settings.update { it.copy(voice = v) }
                    container.changed()
                }
            }
        }
    }
}

/** The voices with a sample line each; shared with onboarding. */
@Composable
fun VoiceList(selected: Voice, seed: Long, onPick: (Voice) -> Unit) {
    Voice.entries.forEach { v ->
        val t = voiceText(v)
        val on = v == selected
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(U.RMd)
                .background(if (on) U.c.accentSoft else U.c.surface)
                .border(if (on) 2.dp else 1.dp, if (on) U.c.accent else U.c.line, U.RMd)
                .clickable { onPick(v) }
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (on) U.c.surface else U.c.surface2), contentAlignment = Alignment.Center) {
                Text(v.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.name, style = UType.Strong)
                Text(t.tagline, style = UType.Muted)
                Spacer(Modifier.height(6.dp))
                Text(praise(t.solid.pick(seed)), style = UType.Quote.copy(fontSize = 15.sp, lineHeight = 21.sp, color = U.c.muted))
            }
            if (on) Icon(Icons.Rounded.CheckCircle, null, tint = U.c.accent, modifier = Modifier.size(22.dp))
        }
    }
}

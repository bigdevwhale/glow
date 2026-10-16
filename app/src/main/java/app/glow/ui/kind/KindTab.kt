package app.glow.ui.kind

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.data.KindWord
import app.glow.domain.Voice
import app.glow.ui.GlowData
import app.glow.ui.Go
import app.glow.ui.dayLabel
import app.glow.ui.voiceText
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.SoftButton
import app.glow.ui.components.UCard
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import java.io.File

@Composable
fun KindTab(data: GlowData, go: Go) {
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.kind_title), style = UType.Title, modifier = Modifier.padding(top = 10.dp))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.kind_sub), style = UType.Sub)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton(stringResource(R.string.kind_add), { go.newKind() }, Modifier.weight(1f), icon = Icons.Rounded.Add)
                if (data.kindWords.size > 1) {
                    SoftButton(stringResource(R.string.kind_shuffle), { go.shuffle() }, Modifier.weight(1f), U.c.gold, U.c.goldSoft, Icons.Rounded.Shuffle)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (data.kindWords.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    GlowOrb(Modifier.height(160.dp).fillMaxWidth(0.5f), strength = 0.3f) { Text("💌", fontSize = 40.sp) }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.kind_empty), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
        items(data.kindWords, key = { it.id }) { k ->
            KindCard(k, Modifier.padding(bottom = 10.dp)) { go.kind(k.id) }
        }
    }
}

/** Who said it: a person's name, or the voice for a saved praise. */
@Composable
fun kindFrom(k: KindWord): String = when {
    k.source == KindWord.SOURCE_GLOW && Voice.entries.any { it.name == k.from } -> Voice.of(k.from).let { it.emoji + " " + voiceText(it).name }
    k.source == KindWord.SOURCE_GLOW -> "✨ " + k.from
    else -> k.from
}

@Composable
fun KindCard(k: KindWord, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val container = LocalContext.current.container
    val image = rememberImage(k.image?.let { File(container.images, it) }, 900)
    UCard(modifier, onClick = onClick, padding = PaddingValues(0.dp)) {
        if (image != null) {
            Image(image, null, Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(U.RMd), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
        }
        Column(Modifier.padding(16.dp)) {
            if (k.text.isNotBlank()) Text(k.text, style = UType.Quote.copy(fontSize = 17.sp, lineHeight = 25.sp))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val from = kindFrom(k)
                if (from.isNotBlank()) Text(stringResource(R.string.kind_from, from), style = UType.Small.copy(color = U.c.accent), modifier = Modifier.weight(1f))
                else Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(dayLabel(java.time.Instant.ofEpochMilli(k.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()), style = UType.Small)
            }
        }
    }
}

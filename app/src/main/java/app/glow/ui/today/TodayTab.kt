package app.glow.ui.today

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.data.KindWord
import app.glow.data.Win
import app.glow.domain.Category
import app.glow.domain.WinSize
import app.glow.domain.pick
import app.glow.ui.GlowData
import app.glow.ui.Go
import app.glow.ui.categoryColor
import app.glow.ui.categoryIcon
import app.glow.ui.content
import app.glow.ui.greeting
import app.glow.ui.gs
import app.glow.ui.praise
import app.glow.ui.shareText
import app.glow.ui.timeOf
import app.glow.ui.voiceText
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.SectionTitle
import app.glow.ui.components.UCard
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

@Composable
fun TodayTab(data: GlowData, go: Go) {
    val s = data.settings
    val stats = data.stats
    val hour = Instant.ofEpochMilli(data.now).atZone(ZoneId.systemDefault()).hour
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(hour) + if (s.name.isNotBlank()) "," else "", style = UType.Sub)
                if (s.name.isNotBlank()) Text(s.name, style = UType.Title)
            }
            SosPill { go.sos() }
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }
        Spacer(Modifier.height(14.dp))

        if (stats.returning && stats.todayWins.isEmpty()) {
            val line = voiceText(s.voice).welcomeBack.pick(stats.today.toEpochDay())
            UCard {
                Text(stringResource(R.string.welcome_back).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
                Spacer(Modifier.height(6.dp))
                Text(praise(line), style = UType.Body)
            }
            Spacer(Modifier.height(12.dp))
        }

        PraiseCard(data)
        Spacer(Modifier.height(14.dp))
        WinButton { go.addWin() }

        SectionTitle(stringResource(R.string.today_wins) + if (stats.todayWins.isNotEmpty()) " · ${stats.todayWins.size}" else "")
        if (stats.todayWins.isEmpty()) {
            Text(gs(R.string.today_empty), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
        } else {
            UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)) {
                stats.todayWins.reversed().forEach { w -> WinRow(w) { go.editWin(w.id) } }
            }
        }

        SectionTitle(stringResource(R.string.set_evening))
        val ev = stats.todayEvening
        if (ev != null) {
            UCard(onClick = { go.evening() }) {
                ev.items.forEachIndexed { i, t ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("${i + 1}.", style = UType.Strong.copy(color = U.c.accent), modifier = Modifier.width(22.dp))
                        Text(t, style = UType.Body)
                    }
                }
            }
        } else {
            ToolCard(Icons.Rounded.Bedtime, stringResource(R.string.evening_title), gs(R.string.evening_cta), U.c.violet, U.c.violetSoft) { go.evening() }
        }

        Spacer(Modifier.height(10.dp))
        ToolCard(Icons.Rounded.RecordVoiceOver, stringResource(R.string.translator), stringResource(R.string.translator_sub), U.c.sky, U.c.skySoft) { go.translator() }
        Spacer(Modifier.height(10.dp))
        ToolCard(Icons.Rounded.Favorite, stringResource(R.string.kind_random), stringResource(R.string.kind_random_sub), U.c.accent, U.c.accentSoft) {
            if (data.kindWords.isEmpty()) go.newKind() else go.shuffle()
        }
    }
}

/** The praise of the day from the chosen voice, with keep, another and share. */
@Composable
private fun PraiseCard(data: GlowData) {
    val context = LocalContext.current
    val container = context.container
    val s = data.settings
    val voice = voiceText(s.voice)
    val content = content()
    val day = data.stats.today.toEpochDay()
    var shift by rememberSaveable { mutableIntStateOf(s.praiseShift) }
    val text = praise(content.daily(s.voice, day + shift))
    val saved = data.kindWords.any { it.source == KindWord.SOURCE_GLOW && it.text == text }
    val savedToast = stringResource(R.string.saved_to_kind)
    val shareTitle = stringResource(R.string.share)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(U.RLg)
            .background(Brush.linearGradient(listOf(U.c.accentSoft, U.c.goldSoft)))
            .border(1.dp, U.c.line, U.RLg),
    ) {
        // A soft sun peeking in from the corner, behind the words.
        GlowOrb(Modifier.size(170.dp).align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp), strength = 0.3f)
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.voice.emoji, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.praise_of_day).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent))
                    Text(voice.name, style = UType.Small)
                }
            }
            Spacer(Modifier.height(14.dp))
            AnimatedContent(text, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "praise") { t ->
                Text(t, style = UType.Quote.copy(fontSize = 21.sp, lineHeight = 29.sp))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SmallAction(if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, stringResource(R.string.keep), active = saved) {
                    if (!saved) {
                        container.write { dao().insertKindWord(KindWord(at = System.currentTimeMillis(), text = text, from = s.voice.name, source = KindWord.SOURCE_GLOW)) }
                        Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                    }
                }
                SmallAction(Icons.Rounded.Refresh, stringResource(R.string.another)) {
                    shift++
                    container.scope.launch {
                        container.settings.update { it.copy(praiseShift = shift) }
                        container.changed()
                    }
                }
                SmallAction(Icons.Rounded.Share, stringResource(R.string.share)) { shareText(context, text, shareTitle) }
            }
        }
    }
}

@Composable
private fun SmallAction(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(U.c.surface.copy(alpha = 0.7f)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (active) U.c.accent else U.c.muted)
        Spacer(Modifier.width(6.dp))
        Text(label, style = UType.Small.copy(color = if (active) U.c.accent else U.c.muted))
    }
}

/** The big glowing "I've got a win!" button. */
@Composable
private fun WinButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(U.RLg)
            .background(Brush.horizontalGradient(listOf(U.c.accent, Color(0xFFF08A4B))))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, null, Modifier.size(28.dp), tint = Color.White)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(gs(R.string.have_win), style = UType.Strong.copy(fontSize = 19.sp, color = Color.White))
            Text(stringResource(R.string.have_win_sub), style = UType.Muted.copy(color = Color.White.copy(alpha = 0.85f)))
        }
    }
}

@Composable
private fun SosPill(onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(U.c.skySoft).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Umbrella, null, Modifier.size(16.dp), tint = U.c.sky)
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.sos), style = UType.Small.copy(color = U.c.sky, fontSize = 13.sp))
    }
}

/** One win in a list: its category stone, the text and the time. */
@Composable
fun WinRow(w: Win, showTime: Boolean = true, trailing: String? = null, onClick: () -> Unit) {
    val cat = Category.of(w.category)
    val color = categoryColor(cat)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val d = when (WinSize.of(w.size)) { WinSize.TINY -> 30.dp; WinSize.SOLID -> 36.dp; WinSize.HUGE -> 42.dp }
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(d).clip(CircleShape).background(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), color))), contentAlignment = Alignment.Center) {
                Icon(categoryIcon(cat), null, Modifier.size(d * 0.5f), tint = Color.White)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(w.text, style = UType.Body, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (showTime) Text(trailing ?: timeOf(w.at), style = UType.Small)
    }
}

@Composable
fun ToolCard(icon: ImageVector, title: String, text: String, color: Color, soft: Color, onClick: () -> Unit) {
    UCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            app.glow.ui.components.IconDisc(icon, 44.dp, color, soft)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(text, style = UType.Muted)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
        }
    }
}

package app.glow.ui.me

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.domain.pick
import app.glow.ui.GlowData
import app.glow.ui.captureInto
import app.glow.ui.categoryColor
import app.glow.ui.categoryLabel
import app.glow.ui.dayLabel
import app.glow.ui.gs
import app.glow.ui.locale
import app.glow.ui.praise
import app.glow.ui.shareImage
import app.glow.ui.voiceText
import app.glow.ui.weekdayShort
import app.glow.ui.components.TopBar
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** "The report on how great you are": one week of wins, shareable as a picture. */
@Composable
fun ReportScreen(data: GlowData, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val stats = data.stats
    val current = stats.weekStart()
    var startDay by rememberSaveable { mutableLongStateOf(current.toEpochDay()) }
    val start = LocalDate.ofEpochDay(startDay)
    val r = stats.report(start)
    val first = data.wins.firstOrNull()?.let { stats.weekStart(stats.dayOf(it.at)) } ?: current
    val shareTitle = stringResource(R.string.share)
    val fmt = DateTimeFormatter.ofPattern("d MMM", locale())

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.report_card), onBack) {
            IconButton(onClick = { scope.launch { shareImage(context, layer, "glow-week", shareTitle) } }) {
                Icon(Icons.Rounded.Share, stringResource(R.string.share), tint = U.c.ink)
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { startDay -= 7 }, enabled = start > first) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = if (start > first) U.c.ink else U.c.line)
            }
            Text(
                start.format(fmt) + " – " + start.plusDays(6).format(fmt),
                style = UType.Strong, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
            IconButton(onClick = { startDay += 7 }, enabled = start < current) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = if (start < current) U.c.ink else U.c.line)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding()) {
            Column(
                Modifier
                    .captureInto(layer)
                    .fillMaxWidth()
                    .clip(U.RLg)
                    .background(Brush.verticalGradient(listOf(U.c.accentSoft, U.c.surface, U.c.goldSoft)))
                    .border(1.dp, U.c.line, U.RLg)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.report_title).uppercase(), style = UType.Eyebrow.copy(color = U.c.accent, textAlign = TextAlign.Center))
                Spacer(Modifier.height(4.dp))
                if (data.settings.name.isNotBlank()) Text(data.settings.name, style = UType.Strong)
                Text(start.format(fmt) + " – " + start.plusDays(6).format(fmt), style = UType.Small)
                Spacer(Modifier.height(14.dp))
                Text("${r.wins}", style = UType.Display.copy(fontSize = 64.sp, lineHeight = 66.sp, color = U.c.accent))
                Text(stringResource(R.string.report_wins), style = UType.Sub)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        r.wins > r.previous -> stringResource(R.string.report_more, r.wins - r.previous)
                        r.wins < r.previous -> stringResource(R.string.report_less)
                        else -> stringResource(R.string.report_same)
                    },
                    style = UType.Small.copy(textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(16.dp))
                WeekBars(r.perDay)
                Spacer(Modifier.height(16.dp))
                if (r.wins == 0) {
                    Text(gs(R.string.report_empty), style = UType.Body.copy(textAlign = TextAlign.Center))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Fact(stringResource(R.string.report_days), "${r.days}/7", Modifier.weight(1f))
                        Fact(stringResource(R.string.report_evenings), "${r.evenings}", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Fact(stringResource(R.string.report_top), r.top?.let { categoryLabel(it) } ?: "—", Modifier.weight(1f), r.top?.let { categoryColor(it) })
                        Fact(
                            stringResource(R.string.report_busiest),
                            r.busiest?.let { dayLabel(it, stats.today) + " · " + r.busiestCount } ?: "—",
                            Modifier.weight(1f),
                        )
                    }
                    if (r.huge.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text(stringResource(R.string.report_huge).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
                        Spacer(Modifier.height(6.dp))
                        r.huge.take(5).forEach { w ->
                            Text("★ " + w.text, style = UType.Body.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.report_verdict).uppercase(), style = UType.Eyebrow)
                Spacer(Modifier.height(6.dp))
                val v = voiceText(data.settings.voice)
                val lines = if (r.wins >= 10) v.huge else if (r.wins > 0) v.solid else v.sos
                Text(praise(lines.pick(start.toEpochDay() / 7)), style = UType.Quote.copy(textAlign = TextAlign.Center))
                Spacer(Modifier.height(4.dp))
                Text(data.settings.voice.emoji + " " + v.name + " · Glow", style = UType.Small)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Column(modifier.clip(U.RMd).background(U.c.surface.copy(alpha = 0.8f)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = UType.Strong.copy(color = color ?: U.c.ink, textAlign = TextAlign.Center), maxLines = 1)
        Text(label, style = UType.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
    }
}

/** Seven bars, Monday first, height by wins that day. */
@Composable
private fun WeekBars(perDay: List<Int>) {
    val accent = U.c.accent
    val soft = U.c.accent.copy(alpha = 0.15f)
    val max = maxOf(perDay.maxOrNull() ?: 0, 3)
    Column {
        Canvas(Modifier.fillMaxWidth().height(80.dp)) {
            val slot = size.width / 7
            val bw = slot * 0.5f
            perDay.forEachIndexed { i, n ->
                val x = i * slot + (slot - bw) / 2
                drawRoundRect(soft, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 2))
                if (n > 0) {
                    val h = size.height * n / max
                    drawRoundRect(accent, Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 2))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            repeat(7) { i -> Text(weekdayShort(i), style = UType.Small.copy(textAlign = TextAlign.Center, fontSize = 11.sp), modifier = Modifier.weight(1f)) }
        }
    }
}

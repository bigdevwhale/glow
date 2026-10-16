package app.glow.ui.jar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.glow.R
import app.glow.data.Win
import app.glow.domain.Category
import app.glow.domain.Voice
import app.glow.domain.WinSize
import app.glow.ui.GlowData
import app.glow.ui.Go
import app.glow.ui.categoryColor
import app.glow.ui.dayLabel
import app.glow.ui.monthLabel
import app.glow.ui.timeOf
import app.glow.ui.today.CategoryChip
import app.glow.ui.today.WinRow
import app.glow.ui.voiceText
import app.glow.ui.components.Jar
import app.glow.ui.components.JarStone
import app.glow.ui.components.SectionTitle
import app.glow.ui.components.UCard
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import java.time.YearMonth

@Composable
fun JarTab(data: GlowData, go: Go) {
    val stats = data.stats
    val thisMonth = YearMonth.from(stats.today)
    var monthKey by rememberSaveable { mutableStateOf(thisMonth.toString()) }
    val month = YearMonth.parse(monthKey)
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<Win?>(null) }
    // The stones only move while the tab is on screen and the app is in front.
    var resumed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        resumed = true
        onPauseOrDispose { resumed = false }
    }
    val wins = stats.winsIn(month.atDay(1), month.atEndOfMonth())
    val first = data.wins.firstOrNull()?.let { YearMonth.from(stats.dayOf(it.at)) } ?: thisMonth
    val list = wins.filter { filter == null || it.category == filter }.reversed()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.jar_title), style = UType.Title)
                    Text(
                        pluralStringResource(R.plurals.wins, wins.size, wins.size) +
                            wins.count { it.size == WinSize.HUGE.value }.let { if (it > 0) " · " + stringResource(R.string.jar_huge_n, it) else "" },
                        style = UType.Sub,
                    )
                }
                IconButton(onClick = { monthKey = month.minusMonths(1).toString() }, enabled = month > first) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_month), tint = if (month > first) U.c.ink else U.c.line)
                }
                Text(monthLabel(month), style = UType.Strong, modifier = Modifier.widthIn(min = 70.dp), textAlign = TextAlign.Center)
                IconButton(onClick = { monthKey = month.plusMonths(1).toString() }, enabled = month < thisMonth) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month), tint = if (month < thisMonth) U.c.ink else U.c.line)
                }
            }
            Spacer(Modifier.height(8.dp))
            val stones = remember(wins) { wins.map { JarStone(it.id, it.size, categoryColor(Category.of(it.category))) } }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Jar(
                    stones,
                    Modifier.fillMaxWidth(0.82f).aspectRatio(0.78f),
                    live = resumed,
                    onTap = { id -> open = wins.firstOrNull { it.id == id } },
                )
                if (wins.isEmpty()) {
                    Text(stringResource(R.string.jar_empty), style = UType.Muted.copy(textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = 70.dp))
                }
            }
            if (wins.isNotEmpty()) {
                Text(stringResource(R.string.jar_shake), style = UType.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            }
            SectionTitle(stringResource(R.string.jar_list))
        }
        if (wins.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    item {
                        Text(
                            stringResource(R.string.jar_all),
                            style = UType.Small.copy(color = if (filter == null) U.c.accent else U.c.muted, fontSize = 13.5.sp),
                            modifier = Modifier.clip(CircleShape).background(if (filter == null) U.c.accentSoft else U.c.surface)
                                .clickable { filter = null }.padding(horizontal = 14.dp, vertical = 9.dp),
                        )
                    }
                    val used = Category.entries.filter { c -> wins.any { it.category == c.id } }
                    items(used) { c -> CategoryChip(c, filter == c.id) { filter = if (filter == c.id) null else c.id } }
                }
            }
        }
        val days = list.groupBy { stats.dayOf(it.at) }
        days.forEach { (day, dayWins) ->
            item(key = day.toString()) {
                Text(dayLabel(day, stats.today), style = UType.Small, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp))
                UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                    dayWins.forEach { w -> WinRow(w) { open = w } }
                }
            }
        }
    }

    open?.let { w -> WinDialog(w, onEdit = { open = null; go.editWin(w.id) }, onDismiss = { open = null }) }
}

/** A stone picked from the jar: what the win was and what was said about it. */
@Composable
fun WinDialog(w: Win, onEdit: () -> Unit, onDismiss: () -> Unit) {
    val cat = Category.of(w.category)
    val color = categoryColor(cat)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RLg).background(U.c.bg).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(dayLabel(java.time.Instant.ofEpochMilli(w.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) + ", " + timeOf(w.at), style = UType.Small)
            }
            Spacer(Modifier.height(10.dp))
            Text(w.text, style = UType.Strong.copy(fontSize = 19.sp))
            if (w.praise.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(w.praise, style = UType.Quote)
                Spacer(Modifier.height(4.dp))
                Text("— " + voiceText(Voice.of(w.voice)).name, style = UType.Small)
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit), color = U.c.muted, style = UType.Button) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = U.c.accent, style = UType.Button) }
            }
        }
    }
}

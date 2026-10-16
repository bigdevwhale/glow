package app.glow.ui.me

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.domain.Award
import app.glow.domain.Order
import app.glow.domain.Orders
import app.glow.ui.DiaryIcon
import app.glow.ui.GlowData
import app.glow.ui.Go
import app.glow.ui.degreeLabel
import app.glow.ui.gs
import app.glow.ui.orderColor
import app.glow.ui.orderIcon
import app.glow.ui.orderName
import app.glow.ui.orderRequirement
import app.glow.ui.today.ToolCard
import app.glow.ui.voiceText
import app.glow.ui.components.Bar
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.Medal
import app.glow.ui.components.SectionTitle
import app.glow.ui.components.UCard
import app.glow.ui.theme.U
import app.glow.ui.theme.UType

@Composable
fun MeTab(data: GlowData, go: Go) {
    val s = data.settings
    val stats = data.stats
    var openOrder by remember { mutableStateOf<Order?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(s.name.ifBlank { stringResource(R.string.tab_me) }, style = UType.Title, modifier = Modifier.weight(1f))
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }
        Spacer(Modifier.height(10.dp))

        // The soft streak: days in a row with at least one win.
        Row(
            Modifier.fillMaxWidth().clip(U.RLg).background(Brush.linearGradient(listOf(U.c.goldSoft, U.c.accentSoft))).border(1.dp, U.c.line, U.RLg).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowOrb(Modifier.size(84.dp), strength = (stats.run / 14f).coerceIn(0.2f, 1f)) {
                Text("${stats.run}", style = UType.Number.copy(fontSize = 24.sp, color = U.c.ink))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.run_title), style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (stats.run > 0 && stats.todayWins.isEmpty()) stringResource(R.string.run_today) else gs(R.string.run_text),
                    style = UType.Muted,
                )
                if (stats.bestRun > 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.run_best, pluralStringResource(R.plurals.days, stats.bestRun, stats.bestRun)), style = UType.Small.copy(color = U.c.gold))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        ToolCard(Icons.Rounded.Insights, stringResource(R.string.report_card), gs(R.string.report_card_text), U.c.gold, U.c.goldSoft) { go.report() }

        SectionTitle(stringResource(R.string.stats))
        val tiles = listOf(
            R.string.stat_wins to stats.wins.size,
            R.string.stat_huge to stats.huge,
            R.string.stat_kind to stats.kindWords.size,
            R.string.stat_evenings to stats.evenings.size,
            R.string.stat_sos to stats.sosCount,
            R.string.stat_reframes to stats.reframes,
        )
        tiles.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Column(
                        Modifier.weight(1f).clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("$value", style = UType.Number)
                        Text(stringResource(label), style = UType.Small.copy(textAlign = TextAlign.Center), maxLines = 2)
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.orders_n, data.earned.size, Orders.all.size))
        Order.entries.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                row.forEach { o ->
                    val degree = o.degree(stats)
                    Column(
                        Modifier.weight(1f).clip(U.RMd).clickable { openOrder = o }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Medal(degree, orderColor(o), size = 66.dp) {
                            Icon(orderIcon(o), null, Modifier.size(18.dp), tint = if (degree > 0) Color.White else U.c.faint)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(3) { i -> Box(Modifier.size(5.dp).clip(CircleShape).background(if (i < degree) U.c.gold else U.c.line)) }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        ToolCard(DiaryIcon, stringResource(R.string.diary), pluralStringResource(R.plurals.days, stats.evenings.size, stats.evenings.size), U.c.violet, U.c.violetSoft) { go.diary() }
        Spacer(Modifier.height(10.dp))
        UCard(onClick = { go.voice() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(U.c.accentSoft), contentAlignment = Alignment.Center) { Text(s.voice.emoji, fontSize = 22.sp) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.voice), style = UType.Muted)
                    Text(voiceText(s.voice).name, style = UType.Strong)
                }
                Text(stringResource(R.string.voice_pick), style = UType.Small.copy(color = U.c.accent))
            }
        }
    }

    openOrder?.let { o -> OrderDialog(o, data, onCertificate = { a -> openOrder = null; go.certificate(a) }, onDismiss = { openOrder = null }) }
}

/** An order's three degrees, what each asks for, progress to the next and links to earned certificates. */
@Composable
private fun OrderDialog(o: Order, data: GlowData, onCertificate: (Award) -> Unit, onDismiss: () -> Unit) {
    val stats = data.stats
    val degree = o.degree(stats)
    val value = o.value(stats)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(U.RLg).background(U.c.bg).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Medal(degree, orderColor(o), size = 100.dp) {
                Icon(orderIcon(o), null, Modifier.size(28.dp), tint = if (degree > 0) Color.White else U.c.faint)
            }
            Spacer(Modifier.height(10.dp))
            Text(orderName(o), style = UType.Title.copy(fontSize = 21.sp, textAlign = TextAlign.Center))
            Spacer(Modifier.height(6.dp))
            val next = o.next(stats)
            if (next != null) {
                Text(stringResource(R.string.order_next, value, next), style = UType.Small)
                Spacer(Modifier.height(6.dp))
                Bar(value.toFloat() / next, color = U.c.gold, track = U.c.goldSoft)
            } else {
                Text(stringResource(R.string.order_maxed), style = UType.Small.copy(color = U.c.gold))
            }
            Spacer(Modifier.height(14.dp))
            (1..3).forEach { d ->
                val earned = d <= degree
                Row(
                    Modifier.fillMaxWidth().clip(U.RSm).then(if (earned) Modifier.clickable { onCertificate(Award(o, d)) } else Modifier).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Medal(if (earned) d else 0, orderColor(o), size = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(degreeLabel(d).replaceFirstChar { it.uppercaseChar() }, style = UType.Strong.copy(fontSize = 14.sp, color = if (earned) U.c.ink else U.c.faint))
                        Text(orderRequirement(o, o.goals[d - 1]), style = UType.Small)
                    }
                    if (earned) Text(stringResource(R.string.certificate), style = UType.Small.copy(color = U.c.accent))
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = U.c.accent, style = UType.Button) }
        }
    }
}

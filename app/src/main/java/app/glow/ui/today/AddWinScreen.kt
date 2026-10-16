package app.glow.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.data.Win
import app.glow.domain.Category
import app.glow.domain.WinSize
import app.glow.domain.personalize
import app.glow.ui.GlowData
import app.glow.ui.Go
import app.glow.ui.LocalPerson
import app.glow.ui.categoryColor
import app.glow.ui.categoryIcon
import app.glow.ui.categoryLabel
import app.glow.ui.content
import app.glow.ui.dayLabel
import app.glow.ui.g
import app.glow.ui.sizeLabel
import app.glow.ui.sizeText
import app.glow.ui.timeOf
import app.glow.ui.components.ConfirmDialog
import app.glow.ui.components.DateTimeDialog
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.SectionTitle
import app.glow.ui.components.TopBar
import app.glow.ui.components.UTextField
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

/** A new win ([id] 0), or an existing one to edit. A new one goes straight to its praise. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddWinScreen(id: Long, presetCategory: String, data: GlowData, go: Go) {
    val container = LocalContext.current.container
    val person = LocalPerson.current
    val content = content()
    var loaded by remember { mutableStateOf<Win?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var size by rememberSaveable { mutableStateOf(WinSize.SOLID) }
    var category by rememberSaveable { mutableStateOf(Category.of(presetCategory)) }
    var at by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(id) {
        if (id == 0L) {
            focus.requestFocus()
            return@LaunchedEffect
        }
        val w = container.db.dao().win(id) ?: return@LaunchedEffect
        loaded = w
        text = w.text
        size = WinSize.of(w.size)
        category = Category.of(w.category)
        at = w.at
    }

    fun save() {
        val t = text.trim()
        if (t.isEmpty()) return
        val old = loaded
        if (old != null) {
            container.write { dao().updateWin(old.copy(text = t, size = size.value, category = category?.id.orEmpty(), at = at)) }
            go.back()
            return
        }
        val v = data.settings.voice
        val lines = content.voice(v).forWin(size)
        // Avoid repeating the praise the last win of this size got.
        val last = data.wins.lastOrNull { it.size == size.value }?.praise
        val options = lines.map { personalize(it, person.name, person.gender) }.filter { it != last }.ifEmpty { lines }
        val praise = options.random()
        container.scope.launch {
            val newId = container.db.dao().insertWin(Win(at = at, text = t, size = size.value, category = category?.id.orEmpty(), praise = praise, voice = v.name))
            container.changed()
            withContext(Dispatchers.Main) { go.praise(newId) }
        }
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(if (id == 0L) "" else stringResource(R.string.edit_win), onBack = { go.back() }) {
            if (loaded != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            if (id == 0L) {
                Text(stringResource(R.string.add_title), style = UType.Title)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.add_hint), style = UType.Sub)
                Spacer(Modifier.height(16.dp))
            }
            UTextField(text, { text = it.take(200) }, Modifier.focusRequester(focus), placeholder = g(content.ideasFor(category).firstOrNull() ?: ""), minLines = 2)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content.ideasFor(category).forEach { idea ->
                    val t = g(idea)
                    Text(
                        t,
                        style = UType.Small.copy(color = U.c.muted),
                        modifier = Modifier.clip(CircleShape).background(U.c.surface2).border(1.dp, U.c.line, CircleShape)
                            .clickable { text = t }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            SectionTitle(stringResource(R.string.add_size))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WinSize.entries.forEach { s ->
                    SizeOption(s, s == size, Modifier.weight(1f)) { size = s }
                }
            }

            SectionTitle(stringResource(R.string.add_category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    CategoryChip(c, c == category) { category = if (category == c) null else c }
                }
            }

            if (loaded != null) {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.clip(U.RSm).clickable { pickDate = true }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp), tint = U.c.muted)
                    Spacer(Modifier.width(8.dp))
                    val day = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
                    Text(dayLabel(day) + ", " + timeOf(at), style = UType.Body.copy(color = U.c.muted))
                }
                if (loaded!!.praise.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(loaded!!.praise, style = UType.Quote.copy(fontSize = 16.sp, color = U.c.muted))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding()) {
            PrimaryButton(
                if (id == 0L) stringResource(R.string.praise_me) else stringResource(R.string.save),
                ::save, Modifier.fillMaxWidth(), enabled = text.isNotBlank(),
                icon = if (id == 0L) Icons.Rounded.AutoAwesome else null,
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            stringResource(R.string.delete_win), stringResource(R.string.delete_win_text), stringResource(R.string.delete),
            onConfirm = {
                loaded?.let { w -> container.write { dao().deleteWin(w) } }
                go.back()
            },
            onDismiss = { confirmDelete = false },
            danger = true,
        )
    }
    if (pickDate) DateTimeDialog(at, onDismiss = { pickDate = false }) { at = it }
}

@Composable
private fun SizeOption(s: WinSize, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = when (s) { WinSize.TINY -> U.c.sage; WinSize.SOLID -> U.c.accent; WinSize.HUGE -> U.c.gold }
    Column(
        modifier
            .clip(U.RMd)
            .background(if (selected) color.copy(alpha = 0.14f) else U.c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) color else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
            val d = when (s) { WinSize.TINY -> 18.dp; WinSize.SOLID -> 28.dp; WinSize.HUGE -> 42.dp }
            Box(Modifier.size(d).clip(CircleShape).background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.6f), color))))
        }
        Spacer(Modifier.height(6.dp))
        Text(sizeLabel(s), style = UType.Strong.copy(color = if (selected) color else U.c.ink))
        Text(sizeText(s), style = UType.Small.copy(textAlign = TextAlign.Center))
    }
}

@Composable
fun CategoryChip(c: Category?, selected: Boolean, onClick: () -> Unit) {
    val color = categoryColor(c)
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) color.copy(alpha = 0.16f) else U.c.surface)
            .border(1.dp, if (selected) color else U.c.line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(categoryIcon(c), null, Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text(categoryLabel(c), style = UType.Small.copy(color = if (selected) color else U.c.muted, fontSize = 13.5.sp))
    }
}

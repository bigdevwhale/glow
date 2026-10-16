package app.glow.ui.today

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.data.KindWord
import app.glow.ui.GlowData
import app.glow.ui.content
import app.glow.ui.g
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.SectionTitle
import app.glow.ui.components.SoftButton
import app.glow.ui.components.TopBar
import app.glow.ui.components.UCard
import app.glow.ui.components.UTextField
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.launch

/** Spots thinking traps in a harsh thought and offers a kinder way to say it. Fully offline, rule-based. */
@Composable
fun TranslatorScreen(data: GlowData, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val focus = LocalFocusManager.current
    val content = content()
    var input by rememberSaveable { mutableStateOf("") }
    var asked by rememberSaveable { mutableStateOf("") }
    var variant by rememberSaveable { mutableIntStateOf(0) }
    var kept by rememberSaveable { mutableStateOf(false) }
    val traps = if (asked.isBlank()) emptyList() else content.traps(asked)
    val options = traps.take(2).flatMap { it.kinder }.ifEmpty { content.generic }
    val kind = g(options[variant % options.size])
    val savedToast = stringResource(R.string.saved_to_kind)
    val from = stringResource(R.string.tr_from)

    fun translate() {
        val t = input.trim()
        if (t.isEmpty()) return
        focus.clearFocus()
        if (t != asked) {
            container.scope.launch { container.settings.update { it.copy(reframes = it.reframes + 1) } }
        }
        asked = t
        variant = 0
        kept = false
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(R.string.translator), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Text(stringResource(R.string.tr_intro), style = UType.Sub)
            Spacer(Modifier.height(14.dp))
            UTextField(input, { input = it.take(300) }, placeholder = stringResource(R.string.tr_hint), minLines = 3)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(stringResource(R.string.tr_go), ::translate, Modifier.fillMaxWidth(), enabled = input.isNotBlank(), color = U.c.sky, icon = Icons.Rounded.Translate)

            AnimatedVisibility(asked.isNotBlank()) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    Text(asked, style = UType.Body.copy(color = U.c.faint, textDecoration = TextDecoration.LineThrough))
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowDownward, null, Modifier.size(22.dp), tint = U.c.sky)
                    }
                    Column(
                        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(U.c.skySoft, U.c.accentSoft)), U.RLg)
                            .border(1.dp, U.c.line, U.RLg).padding(20.dp),
                    ) {
                        Text(stringResource(R.string.tr_kind).uppercase(), style = UType.Eyebrow.copy(color = U.c.sky))
                        Spacer(Modifier.height(8.dp))
                        AnimatedContent(kind, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "kind") { k ->
                            Text(k, style = UType.Quote.copy(fontSize = 20.sp, lineHeight = 28.sp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SoftButton(stringResource(R.string.tr_another), { variant++; kept = false }, Modifier.weight(1f), U.c.sky, U.c.skySoft, Icons.Rounded.Refresh)
                        SoftButton(
                            stringResource(R.string.keep), {
                                if (!kept) {
                                    kept = true
                                    container.write { dao().insertKindWord(KindWord(at = System.currentTimeMillis(), text = kind, from = from, source = KindWord.SOURCE_GLOW)) }
                                    Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            Modifier.weight(1f), icon = if (kept) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        )
                    }

                    SectionTitle(stringResource(R.string.tr_traps))
                    if (traps.isEmpty()) {
                        Text(stringResource(R.string.tr_none), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
                    } else {
                        traps.forEach { t ->
                            UCard(Modifier.padding(bottom = 8.dp)) {
                                Text(g(t.title), style = UType.Strong.copy(color = U.c.sky))
                                Spacer(Modifier.height(4.dp))
                                Text(g(t.about), style = UType.Muted)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { input = ""; asked = ""; kept = false }) {
                        Text(stringResource(R.string.tr_again), color = U.c.sky, style = UType.Button)
                    }
                }
            }
            Spacer(Modifier.height(24.dp).navigationBarsPadding())
        }
    }
}

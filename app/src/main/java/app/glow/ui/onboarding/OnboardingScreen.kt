package app.glow.ui.onboarding

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
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.container
import app.glow.domain.Gender
import app.glow.domain.Voice
import app.glow.ui.LocalPerson
import app.glow.ui.Person
import app.glow.ui.clockTime
import app.glow.ui.locale
import app.glow.ui.components.GlowOrb
import app.glow.ui.components.IconDisc
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.TimeDialog
import app.glow.ui.components.UTextField
import app.glow.ui.me.VoiceList
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val PAGES = 4

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val container = LocalContext.current.container
    var page by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<Gender?>(null) }
    var voice by rememberSaveable { mutableStateOf(Voice.FRIEND) }
    var morningOn by rememberSaveable { mutableStateOf(true) }
    var morningAt by rememberSaveable { mutableIntStateOf(9 * 60) }
    var eveningOn by rememberSaveable { mutableStateOf(true) }
    var eveningAt by rememberSaveable { mutableIntStateOf(21 * 60 + 30) }
    var pick by rememberSaveable { mutableIntStateOf(0) }
    val ru = locale().language == "ru"

    fun finish() {
        container.scope.launch {
            container.settings.update {
                it.copy(
                    onboarded = true, name = name.trim(), gender = gender ?: Gender.FEMALE, voice = voice,
                    morningOn = morningOn, morningAt = morningAt, eveningOn = eveningOn, eveningAt = eveningAt,
                )
            }
            container.changed()
        }
        onDone()
    }

    // Preview the voices with the name and gender chosen so far.
    CompositionLocalProvider(LocalPerson provides Person(name.trim(), gender ?: Gender.FEMALE)) {
        Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(PAGES) { i ->
                    Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i <= page) U.c.accent else U.c.line))
                }
            }
            AnimatedContent(page, Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { p ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                    when (p) {
                        0 -> Welcome()
                        1 -> {
                            Title(stringResource(R.string.ob_name), stringResource(R.string.ob_name_text))
                            UTextField(name, { name = it.take(30) }, placeholder = stringResource(R.string.ob_name_hint))
                            // Only Russian changes word forms with gender; English has nothing to ask.
                            if (ru) {
                                Spacer(Modifier.height(26.dp))
                                Title(stringResource(R.string.ob_gender), stringResource(R.string.ob_gender_text))
                                Option(stringResource(R.string.gender_female), gender == Gender.FEMALE) { gender = Gender.FEMALE }
                                Option(stringResource(R.string.gender_male), gender == Gender.MALE) { gender = Gender.MALE }
                            }
                        }
                        2 -> {
                            Title(stringResource(R.string.ob_voice), stringResource(R.string.ob_voice_text))
                            VoiceList(voice, LocalDate.now().toEpochDay()) { voice = it }
                        }
                        else -> {
                            Title(stringResource(R.string.ob_times), stringResource(R.string.ob_times_text))
                            TimeRow(Icons.Rounded.WbSunny, stringResource(R.string.set_morning), clockTime(morningAt), morningOn, { morningOn = it }) { pick = 1 }
                            Spacer(Modifier.height(10.dp))
                            TimeRow(Icons.Rounded.Bedtime, stringResource(R.string.set_evening), clockTime(eveningAt), eveningOn, { eveningOn = it }) { pick = 2 }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                PrimaryButton(
                    stringResource(
                        when (page) {
                            0 -> R.string.ob_begin
                            PAGES - 1 -> R.string.ob_finish
                            else -> R.string.next
                        }
                    ),
                    { if (page < PAGES - 1) page++ else finish() },
                    Modifier.fillMaxWidth(),
                    enabled = page != 1 || ((gender != null || !ru) && name.isNotBlank()),
                )
                if (page > 0) {
                    TextButton(onClick = { page-- }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.back), color = U.c.muted, style = UType.Button)
                    }
                }
            }
        }
    }
    when (pick) {
        1 -> TimeDialog(morningAt, { pick = 0 }) { morningAt = it }
        2 -> TimeDialog(eveningAt, { pick = 0 }) { eveningAt = it }
    }
}

@Composable
private fun Welcome() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        GlowOrb(Modifier.size(200.dp), strength = 0.8f)
        Text("Glow", style = UType.Display.copy(fontSize = 40.sp, lineHeight = 44.sp))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ob_tagline), style = UType.Sub.copy(textAlign = TextAlign.Center))
    }
    Spacer(Modifier.height(26.dp))
    Feature(Icons.Rounded.AutoAwesome, R.string.ob_f1_title, R.string.ob_f1_text)
    Feature(Icons.Rounded.WbSunny, R.string.ob_f2_title, R.string.ob_f2_text)
    Feature(Icons.Rounded.Umbrella, R.string.ob_f3_title, R.string.ob_f3_text)
    Feature(Icons.Rounded.Lock, R.string.ob_f4_title, R.string.ob_f4_text)
}

@Composable
private fun Feature(icon: ImageVector, title: Int, text: Int) {
    Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        IconDisc(icon, 44.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(title), style = UType.Strong)
            Text(stringResource(text), style = UType.Muted)
        }
    }
}

@Composable
private fun Title(title: String, text: String?) {
    Text(title, style = UType.Title)
    if (text != null) {
        Spacer(Modifier.height(6.dp))
        Text(text, style = UType.Sub)
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun Option(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(U.RMd)
            .background(if (selected) U.c.accentSoft else U.c.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) U.c.accent else U.c.line, U.RMd)
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Text(label, style = UType.Strong.copy(color = if (selected) U.c.accent else U.c.ink))
    }
}

@Composable
private fun TimeRow(icon: ImageVector, title: String, time: String, on: Boolean, onToggle: (Boolean) -> Unit, onPick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).border(1.dp, U.c.line, U.RMd).clickable(onClick = onPick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            Text(time, style = UType.Number.copy(color = if (on) U.c.accent else U.c.faint))
        }
        Switch(on, onToggle, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
    }
}

package app.glow.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.glow.R
import app.glow.ui.GlowData
import app.glow.ui.dayLabel
import app.glow.ui.components.TopBar
import app.glow.ui.components.UCard
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import java.time.LocalDate

/** Every evening of pride, newest first. */
@Composable
fun DiaryScreen(data: GlowData, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.diary), onBack)
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp)) {
            if (data.evenings.isEmpty()) item { Text(stringResource(R.string.diary_empty), style = UType.Muted, modifier = Modifier.padding(4.dp)) }
            items(data.evenings, key = { it.day }) { e ->
                Text(dayLabel(LocalDate.ofEpochDay(e.day), data.stats.today), style = UType.Small, modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 6.dp))
                UCard {
                    e.items.forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 3.dp)) {
                            Text("${i + 1}.", style = UType.Strong.copy(color = U.c.violet), modifier = Modifier.width(22.dp))
                            Text(t, style = UType.Body)
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

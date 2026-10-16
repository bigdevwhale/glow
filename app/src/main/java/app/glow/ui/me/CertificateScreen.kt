package app.glow.ui.me

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.domain.Award
import app.glow.ui.GlowData
import app.glow.ui.awardTitle
import app.glow.ui.captureInto
import app.glow.ui.dateLong
import app.glow.ui.gs
import app.glow.ui.orderColor
import app.glow.ui.orderIcon
import app.glow.ui.orderRequirement
import app.glow.ui.shareImage
import app.glow.ui.components.Medal
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.TopBar
import app.glow.ui.theme.Lora
import app.glow.ui.theme.U
import app.glow.ui.theme.UType
import kotlinx.coroutines.launch

private val Paper = Color(0xFFFFFBF2)
private val PaperInk = Color(0xFF3A2A1E)
private val PaperMuted = Color(0xFF8A7563)
private val Gilt = Color(0xFFC8962A)

/** A certificate of honour for one degree of an order, on cream paper in both themes, shareable as a picture. */
@Composable
fun CertificateScreen(award: Award, data: GlowData, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val shareTitle = stringResource(R.string.share)
    val name = data.settings.name

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.certificate), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Column(
                Modifier
                    .captureInto(layer)
                    .fillMaxWidth()
                    .background(Paper)
                    .padding(10.dp)
                    .border(2.dp, Gilt)
                    .padding(5.dp)
                    .border(1.dp, Gilt.copy(alpha = 0.6f))
                    .padding(horizontal = 20.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✦", color = Gilt, fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.cert_heading).uppercase(), style = UType.Eyebrow.copy(color = Gilt, fontSize = 13.sp))
                    Spacer(Modifier.width(10.dp))
                    Text("✦", color = Gilt, fontSize = 14.sp)
                }
                Spacer(Modifier.height(18.dp))
                Medal(award.degree, orderColor(award.order), size = 120.dp) {
                    Icon(orderIcon(award.order), null, Modifier.size(32.dp), tint = Color.White)
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.cert_certifies), style = UType.Body.copy(color = PaperMuted, fontFamily = Lora, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center))
                Spacer(Modifier.height(6.dp))
                Text(
                    name.ifBlank { gs(R.string.cert_bearer) },
                    style = UType.Display.copy(color = PaperInk, fontSize = if (name.isBlank()) 22.sp else 32.sp, textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(6.dp))
                Text(gs(R.string.cert_awarded), style = UType.Body.copy(color = PaperMuted, fontFamily = Lora, fontStyle = FontStyle.Italic))
                Spacer(Modifier.height(8.dp))
                Text(awardTitle(award), style = UType.Title.copy(color = PaperInk, fontSize = 22.sp, lineHeight = 28.sp, fontFamily = Lora, textAlign = TextAlign.Center))
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.cert_for, orderRequirement(award.order, award.order.goals[award.degree - 1])),
                    style = UType.Sub.copy(color = PaperMuted, textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.width(160.dp).height(1.dp).background(Gilt.copy(alpha = 0.6f)))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.cert_sign), style = UType.Small.copy(color = PaperMuted, textAlign = TextAlign.Center))
                Text(dateLong(data.stats.today), style = UType.Small.copy(color = PaperMuted))
            }
            Spacer(Modifier.height(20.dp))
        }
        PrimaryButton(
            stringResource(R.string.share),
            { scope.launch { shareImage(context, layer, "glow-certificate", shareTitle) } },
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding(),
            icon = Icons.Rounded.Share,
        )
    }
}

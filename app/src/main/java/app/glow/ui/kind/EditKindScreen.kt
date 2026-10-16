package app.glow.ui.kind

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.glow.R
import app.glow.container
import app.glow.data.KindWord
import app.glow.ui.GlowData
import app.glow.ui.components.ConfirmDialog
import app.glow.ui.components.PrimaryButton
import app.glow.ui.components.SoftButton
import app.glow.ui.components.TopBar
import app.glow.ui.components.UTextField
import app.glow.ui.theme.U
import kotlinx.coroutines.launch
import java.io.File

/** New kind words ([id] 0) or an existing entry: the words, who said them and an optional screenshot. */
@Composable
fun EditKindScreen(id: Long, data: GlowData, onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val existing = data.kindWords.firstOrNull { it.id == id }
    var text by rememberSaveable { mutableStateOf(existing?.text.orEmpty()) }
    var from by rememberSaveable { mutableStateOf(if (existing?.source == KindWord.SOURCE_GLOW) "" else existing?.from.orEmpty()) }
    var image by rememberSaveable { mutableStateOf(existing?.image) }
    var confirmDelete by remember { mutableStateOf(false) }
    val glow = existing?.source == KindWord.SOURCE_GLOW
    val created = remember { mutableListOf<String>() }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) container.scope.launch {
            Images.import(context, uri, container.images)?.let { name -> created += name; image = name }
        }
    }
    fun save() {
        val t = text.trim()
        if (t.isEmpty() && image == null) return
        val keep = image
        val stale = (created + listOfNotNull(existing?.image)).filter { it != keep }
        container.write {
            if (existing == null) dao().insertKindWord(KindWord(at = System.currentTimeMillis(), text = t, from = from.trim(), image = keep))
            else dao().updateKindWord(existing.copy(text = t, from = if (glow) existing.from else from.trim(), image = keep))
            stale.forEach { File(container.images, it).delete() }
        }
        onDone()
    }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding().imePadding()) {
        TopBar(stringResource(if (existing == null) R.string.kind_new else R.string.kind_edit), onBack = {
            // Screenshots picked but never saved don't stay behind.
            created.forEach { File(container.images, it).delete() }
            onDone()
        }) {
            if (existing != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = U.c.muted) }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            UTextField(text, { text = it.take(1000) }, placeholder = stringResource(R.string.kind_text_hint), minLines = 4)
            if (!glow) {
                Spacer(Modifier.height(10.dp))
                UTextField(from, { from = it.take(60) }, placeholder = stringResource(R.string.kind_from_hint))
            }
            Spacer(Modifier.height(14.dp))
            val bitmap = rememberImage(image?.let { File(container.images, it) })
            if (bitmap != null) {
                Image(bitmap, null, Modifier.fillMaxWidth().heightIn(max = 420.dp).clip(U.RMd), contentScale = ContentScale.Fit)
                Spacer(Modifier.height(8.dp))
                SoftButton(stringResource(R.string.kind_photo_remove), { image = null }, Modifier.fillMaxWidth(), U.c.muted, U.c.surface2, Icons.Rounded.HideImage)
            } else {
                SoftButton(
                    stringResource(R.string.kind_photo),
                    { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    Modifier.fillMaxWidth(), icon = Icons.Rounded.AddPhotoAlternate,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding(), contentAlignment = Alignment.Center) {
            PrimaryButton(stringResource(R.string.save), ::save, Modifier.fillMaxWidth(), enabled = text.isNotBlank() || image != null)
        }
    }

    if (confirmDelete && existing != null) {
        ConfirmDialog(
            stringResource(R.string.kind_delete), stringResource(R.string.kind_delete_text), stringResource(R.string.delete),
            onConfirm = {
                container.write {
                    dao().deleteKindWord(existing)
                    (created + listOfNotNull(existing.image)).forEach { File(container.images, it).delete() }
                }
                onDone()
            },
            onDismiss = { confirmDelete = false },
            danger = true,
        )
    }
}

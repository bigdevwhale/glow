package app.glow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import app.glow.R
import app.glow.domain.Category
import app.glow.domain.Order
import app.glow.domain.Voice

/** The painted portrait of a voice (tools/gen_art.py). */
fun voicePic(v: Voice): Int = when (v) {
    Voice.FRIEND -> R.drawable.voice_friend
    Voice.GRANNY -> R.drawable.voice_granny
    Voice.COMMENTATOR -> R.drawable.voice_commentator
    Voice.MENTOR -> R.drawable.voice_mentor
    Voice.BUTLER -> R.drawable.voice_butler
}

fun categoryPic(c: Category?): Int = when (c) {
    Category.HOME -> R.drawable.cat_home
    Category.BODY -> R.drawable.cat_body
    Category.WORK -> R.drawable.cat_work
    Category.MIND -> R.drawable.cat_mind
    Category.PEOPLE -> R.drawable.cat_people
    Category.BRAVE -> R.drawable.cat_brave
    Category.CARE -> R.drawable.cat_care
    null -> R.drawable.cat_none
}

fun orderPic(o: Order): Int = when (o) {
    Order.WINS -> R.drawable.order_wins
    Order.TINY -> R.drawable.order_tiny
    Order.HUGE -> R.drawable.order_huge
    Order.RUN -> R.drawable.order_run
    Order.EVENING -> R.drawable.order_evening
    Order.KIND -> R.drawable.order_kind
    Order.SOS -> R.drawable.order_sos
    Order.REFRAME -> R.drawable.order_reframe
    Order.EARLY -> R.drawable.order_early
    Order.HOME -> R.drawable.order_home
    Order.BODY -> R.drawable.order_body
    Order.WORK -> R.drawable.order_work
    Order.MIND -> R.drawable.order_mind
    Order.PEOPLE -> R.drawable.order_people
    Order.BRAVE -> R.drawable.order_brave
    Order.CARE -> R.drawable.order_care
}

@Composable
fun VoiceFace(v: Voice, size: Dp, modifier: Modifier = Modifier) =
    Image(painterResource(voicePic(v)), null, modifier.size(size))

@Composable
fun CategoryPic(c: Category?, size: Dp, modifier: Modifier = Modifier) =
    Image(painterResource(categoryPic(c)), null, modifier.size(size))

/**
 * An order's painted medal: bronze for III, silver for II, the painted gold for I;
 * not yet earned (0) it's a pale grey ghost.
 */
@Composable
fun OrderMedal(o: Order, degree: Int, size: Dp, modifier: Modifier = Modifier) {
    val filter = remember(degree) {
        when (degree) {
            0 -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            1 -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                .62f, .25f, .08f, 0f, 18f,
                .22f, .48f, .06f, 0f, 2f,
                .14f, .18f, .30f, 0f, -6f,
                0f, 0f, 0f, 1f, 0f,
            )))
            2 -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                .30f, .52f, .12f, 0f, 22f,
                .30f, .54f, .12f, 0f, 24f,
                .32f, .56f, .14f, 0f, 34f,
                0f, 0f, 0f, 1f, 0f,
            )))
            else -> null
        }
    }
    Image(painterResource(orderPic(o)), null, modifier.size(size), colorFilter = filter, alpha = if (degree == 0) .45f else 1f)
}

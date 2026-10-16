package app.glow.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.glow.R
import app.glow.content.Content
import app.glow.content.VoiceText
import app.glow.domain.Award
import app.glow.domain.Category
import app.glow.domain.Gender
import app.glow.domain.Order
import app.glow.domain.Voice
import app.glow.domain.WinSize
import app.glow.domain.gendered
import app.glow.domain.personalize
import app.glow.ui.theme.CategoryColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Who the UI is talking to: gender picks Russian word forms, the name goes into praise. */
data class Person(val name: String, val gender: Gender)

val LocalPerson = staticCompositionLocalOf { Person("", Gender.FEMALE) }

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun content(): Content = Content.of(locale())

@Composable
fun voiceText(v: Voice): VoiceText = content().voice(v)

/** A string resource with its "{male|female}" forms resolved. */
@Composable
fun gs(@StringRes id: Int, vararg args: Any): String = gendered(stringResource(id, *args), LocalPerson.current.gender)

@Composable
fun gp(@PluralsRes id: Int, count: Int, vararg args: Any): String = gendered(pluralStringResource(id, count, *args), LocalPerson.current.gender)

/** Gender forms resolved, without the name. */
@Composable
fun g(text: String): String = gendered(text, LocalPerson.current.gender)

/** A praise line with the name and gender forms filled in. */
@Composable
fun praise(template: String): String = LocalPerson.current.let { personalize(template, it.name, it.gender) }

@Composable
fun greeting(hour: Int): String = stringResource(
    when (hour) {
        in 5..11 -> R.string.greet_morning
        in 12..17 -> R.string.greet_day
        in 18..22 -> R.string.greet_evening
        else -> R.string.greet_night
    }
)

@Composable
fun clockTime(minutes: Int): String =
    LocalTime.of(Math.floorMod(minutes, 1440) / 60, Math.floorMod(minutes, 1440) % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()))

@Composable
fun timeOf(at: Long): String =
    Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()))

/** "Today", "Yesterday", or "Mon, 14 Oct". */
@Composable
fun dayLabel(day: LocalDate, today: LocalDate = LocalDate.now()): String = when (day) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> day.format(DateTimeFormatter.ofPattern(if (day.year == today.year) "EEE, d MMM" else "d MMM yyyy", locale()))
}

@Composable
fun dateLong(day: LocalDate): String = day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale()))

@Composable
fun monthLabel(month: java.time.YearMonth): String {
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale()).replaceFirstChar { it.uppercaseChar() }
    return if (month.year == LocalDate.now().year) name else "$name ${month.year}"
}

@Composable
fun weekdayShort(i: Int): String = java.time.DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT_STANDALONE, locale())

@Composable
fun categoryLabel(c: Category?): String = stringResource(
    when (c) {
        Category.HOME -> R.string.cat_home
        Category.BODY -> R.string.cat_body
        Category.WORK -> R.string.cat_work
        Category.MIND -> R.string.cat_mind
        Category.PEOPLE -> R.string.cat_people
        Category.BRAVE -> R.string.cat_brave
        Category.CARE -> R.string.cat_care
        null -> R.string.cat_none
    }
)

fun categoryIcon(c: Category?): ImageVector = when (c) {
    Category.HOME -> Icons.Rounded.Home
    Category.BODY -> Icons.Rounded.SelfImprovement
    Category.WORK -> Icons.Rounded.Work
    Category.MIND -> Icons.Rounded.Psychology
    Category.PEOPLE -> Icons.Rounded.Groups
    Category.BRAVE -> Icons.Rounded.Bolt
    Category.CARE -> Icons.Rounded.Spa
    null -> Icons.Rounded.AutoAwesome
}

fun categoryColor(c: Category?): Color = CategoryColors[c?.ordinal ?: CategoryColors.lastIndex]

@Composable
fun sizeLabel(s: WinSize): String = stringResource(
    when (s) {
        WinSize.TINY -> R.string.size_tiny
        WinSize.SOLID -> R.string.size_solid
        WinSize.HUGE -> R.string.size_huge
    }
)

@Composable
fun sizeText(s: WinSize): String = stringResource(
    when (s) {
        WinSize.TINY -> R.string.size_tiny_text
        WinSize.SOLID -> R.string.size_solid_text
        WinSize.HUGE -> R.string.size_huge_text
    }
)

@Composable
fun orderName(o: Order): String = stringResource(
    when (o) {
        Order.WINS -> R.string.order_wins
        Order.TINY -> R.string.order_tiny
        Order.HUGE -> R.string.order_huge
        Order.RUN -> R.string.order_run
        Order.EVENING -> R.string.order_evening
        Order.KIND -> R.string.order_kind
        Order.SOS -> R.string.order_sos
        Order.REFRAME -> R.string.order_reframe
        Order.EARLY -> R.string.order_early
        Order.HOME -> R.string.order_home
        Order.BODY -> R.string.order_body
        Order.WORK -> R.string.order_work
        Order.MIND -> R.string.order_mind
        Order.PEOPLE -> R.string.order_people
        Order.BRAVE -> R.string.order_brave
        Order.CARE -> R.string.order_care
    }
)

fun orderIcon(o: Order): ImageVector = o.category?.let { categoryIcon(it) } ?: when (o) {
    Order.WINS -> Icons.Rounded.EmojiEvents
    Order.TINY -> Icons.Rounded.Grain
    Order.HUGE -> Icons.Rounded.Star
    Order.RUN -> Icons.Rounded.LocalFireDepartment
    Order.EVENING -> Icons.Rounded.Bedtime
    Order.KIND -> Icons.Rounded.Favorite
    Order.SOS -> Icons.Rounded.Umbrella
    Order.REFRAME -> Icons.Rounded.RecordVoiceOver
    Order.EARLY -> Icons.Rounded.WbSunny
    else -> Icons.Rounded.Shield
}

fun orderColor(o: Order): Color = o.category?.let { categoryColor(it) } ?: when (o) {
    Order.WINS -> Color(0xFFDB3F7A)
    Order.TINY -> Color(0xFF2FA5A0)
    Order.HUGE -> Color(0xFFD08A10)
    Order.RUN -> Color(0xFFEE6A2C)
    Order.EVENING -> Color(0xFF5A5FD0)
    Order.KIND -> Color(0xFFD9406E)
    Order.SOS -> Color(0xFF4A86D9)
    Order.REFRAME -> Color(0xFF8D63D6)
    Order.EARLY -> Color(0xFFE0A526)
    else -> Color(0xFF888888)
}

/** What a degree asks for, e.g. "25 tiny wins noticed". */
@Composable
fun orderRequirement(o: Order, goal: Int): String = when (o) {
    Order.WINS -> gp(R.plurals.req_wins, goal, goal)
    Order.TINY -> gp(R.plurals.req_tiny, goal, goal)
    Order.HUGE -> gp(R.plurals.req_huge, goal, goal)
    Order.RUN -> gp(R.plurals.req_run, goal, goal)
    Order.EVENING -> gp(R.plurals.req_evening, goal, goal)
    Order.KIND -> gp(R.plurals.req_kind, goal, goal)
    Order.SOS -> gp(R.plurals.req_sos, goal, goal)
    Order.REFRAME -> gp(R.plurals.req_reframe, goal, goal)
    Order.EARLY -> gp(R.plurals.req_early, goal, goal)
    else -> gp(R.plurals.req_category, goal, goal, categoryLabel(o.category).lowercase(locale()))
}

@Composable
fun degreeLabel(d: Int): String = stringResource(
    when (d) {
        3 -> R.string.degree_3
        2 -> R.string.degree_2
        else -> R.string.degree_1
    }
)

@Composable
fun awardTitle(a: Award): String = stringResource(R.string.order_title, orderName(a.order), degreeLabel(a.degree))

val SavedIcon = Icons.Rounded.Favorite
val SaveIcon = Icons.Rounded.FavoriteBorder
val DiaryIcon = Icons.AutoMirrored.Rounded.MenuBook

/** A happy buzz that grows with the win: a tap, a double tap, a little drum roll. */
object Haptics {
    fun praise(context: Context, size: WinSize) {
        val (timings, amps) = when (size) {
            WinSize.TINY -> longArrayOf(0, 25) to intArrayOf(0, 120)
            WinSize.SOLID -> longArrayOf(0, 30, 70, 40) to intArrayOf(0, 140, 0, 200)
            WinSize.HUGE -> longArrayOf(0, 30, 50, 30, 50, 30, 80, 120) to intArrayOf(0, 120, 0, 160, 0, 200, 0, 255)
        }
        vibrate(context, timings, amps)
    }

    /** A soft heartbeat, for the SOS screen. */
    fun heartbeat(context: Context) = vibrate(context, longArrayOf(0, 35, 110, 50), intArrayOf(0, 70, 0, 110))

    private fun vibrate(context: Context, timings: LongArray, amps: IntArray) {
        val v = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        if (v == null || !v.hasVibrator()) return
        v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
    }
}

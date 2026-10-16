package app.glow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.glow.R
import app.glow.data.ThemeMode

/** Soft light on blush paper; at night, a warm glow in the dark. */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val gold: Color,
    val goldSoft: Color,
    val sage: Color,
    val sageSoft: Color,
    val sky: Color,
    val skySoft: Color,
    val violet: Color,
    val violetSoft: Color,
    val rose: Color,
    val roseSoft: Color,
    val danger: Color,
    val dark: Boolean,
)

val LightPalette = Palette(
    bg = Color(0xFFFBF5F3),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFFDF9F8),
    line = Color(0xFFF0E3E1),
    ink = Color(0xFF2A1C22),
    muted = Color(0xFF7A6870),
    faint = Color(0xFFB2A1A8),
    accent = Color(0xFFDB3F7A),
    accentSoft = Color(0xFFFCE4EC),
    onAccent = Color(0xFFFFFFFF),
    gold = Color(0xFFD08A10),
    goldSoft = Color(0xFFFCF0D5),
    sage = Color(0xFF3F9574),
    sageSoft = Color(0xFFE0F0E8),
    sky = Color(0xFF3E83C4),
    skySoft = Color(0xFFE0ECF8),
    violet = Color(0xFF7A5BD2),
    violetSoft = Color(0xFFECE8FB),
    rose = Color(0xFFE5602A),
    roseSoft = Color(0xFFFCE7DC),
    danger = Color(0xFFD9443F),
    dark = false,
)

val DarkPalette = Palette(
    bg = Color(0xFF161014),
    surface = Color(0xFF20181D),
    surface2 = Color(0xFF261D23),
    line = Color(0xFF362A31),
    ink = Color(0xFFF5ECEF),
    muted = Color(0xFFB9A7AF),
    faint = Color(0xFF7D6B73),
    accent = Color(0xFFFF7AA8),
    accentSoft = Color(0xFF3D1E2B),
    onAccent = Color(0xFF2A0E19),
    gold = Color(0xFFF2C14E),
    goldSoft = Color(0xFF382D14),
    sage = Color(0xFF6CCBA5),
    sageSoft = Color(0xFF1C3329),
    sky = Color(0xFF7DB6F0),
    skySoft = Color(0xFF1A2A3C),
    violet = Color(0xFFA397FF),
    violetSoft = Color(0xFF2C2750),
    rose = Color(0xFFFF8A4C),
    roseSoft = Color(0xFF3A2418),
    danger = Color(0xFFFF6B63),
    dark = true,
)

/** Stones in the jar, one colour per category; the last is for wins with no category. */
val CategoryColors = listOf(
    Color(0xFFE0A526), // home
    Color(0xFF3FA57A), // body
    Color(0xFF4A86D9), // work
    Color(0xFF8D63D6), // mind
    Color(0xFFEE6A2C), // people
    Color(0xFFD9406E), // brave
    Color(0xFF2FA5A0), // care
    Color(0xFFE8A0B8), // none
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** A soft serif for tips and celebrations. */
val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
    Font(R.font.lora_italic, FontWeight.Normal, FontStyle.Italic),
)

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(28.dp)
    val RLg = RoundedCornerShape(24.dp)
    val RMd = RoundedCornerShape(18.dp)
    val RSm = RoundedCornerShape(12.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontWeight = FontWeight.Medium, fontSize = 32.sp, letterSpacing = (-0.01).em, lineHeight = 38.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.025).em, lineHeight = 32.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.02).em, color = U.c.ink)
    val Timer: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, letterSpacing = (-0.03).em, color = U.c.ink, fontFeatureSettings = "tnum")
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val Quote: TextStyle @Composable get() = TextStyle(fontFamily = Lora, fontSize = 19.sp, lineHeight = 27.sp, color = U.c.ink)
}

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun GlowTheme(dark: Boolean, content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = scheme.copy(
                primary = p.accent,
                onPrimary = p.onAccent,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondaryContainer = p.accentSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}

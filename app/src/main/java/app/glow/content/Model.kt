package app.glow.content

import app.glow.domain.Category
import app.glow.domain.Voice
import app.glow.domain.WinSize
import app.glow.domain.pick
import java.util.Locale

/**
 * Everything one voice says. Lines may hold "{n}" for the user's name, see [app.glow.domain.personalize].
 * Russian lines are gender-neutral: no past-tense verbs about the user.
 */
class VoiceText(
    val name: String,
    /** One line under the name in the voice picker. */
    val tagline: String,
    /** The praise of the day; one per day, in order. */
    val daily: List<String>,
    val tiny: List<String>,
    val solid: List<String>,
    val huge: List<String>,
    /** Cycled on the "I feel bad" screen. */
    val sos: List<String>,
    /** After the three things of the evening are saved. */
    val evening: List<String>,
    /** On the home screen after a gap of a few days. */
    val welcomeBack: List<String>,
) {
    fun forWin(size: WinSize): List<String> = when (size) {
        WinSize.TINY -> tiny
        WinSize.SOLID -> solid
        WinSize.HUGE -> huge
    }
}

/** A thinking trap the translator can spot. A [stems] entry matches when the lowercased text contains it. */
class Trap(
    val id: String,
    val title: String,
    /** What the trap does, in one or two sentences. */
    val about: String,
    val stems: List<String>,
    /** Kinder ways to say the same thing. */
    val kinder: List<String>,
)

class Content(
    val voices: Map<Voice, VoiceText>,
    val traps: List<Trap>,
    /** Kind versions for thoughts no trap matched. */
    val generic: List<String>,
    /** Quick ideas for a win, per category; "" holds ideas with no category. */
    val ideas: Map<String, List<String>>,
) {
    fun voice(v: Voice): VoiceText = voices.getValue(v)

    fun daily(v: Voice, day: Long): String = voice(v).daily.pick(day)

    /** Traps found in [text], strongest first: the one with the most matching stems. */
    fun traps(text: String): List<Trap> {
        val t = " " + text.lowercase(Locale.ROOT).replace('ё', 'е') + " "
        return traps
            .map { trap -> trap to trap.stems.count { t.contains(it) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun ideasFor(c: Category?): List<String> = ideas[c?.id ?: ""].orEmpty()

    companion object {
        fun of(locale: Locale): Content = if (locale.language == "ru") ContentRu else ContentEn
    }
}

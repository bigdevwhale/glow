package app.glow.domain

/** Who does the praising. Each has its own words in [app.glow.content.Content]. */
enum class Voice(val emoji: String) {
    FRIEND("🤗"),
    GRANNY("👵"),
    COMMENTATOR("🎙️"),
    MENTOR("🦉"),
    BUTLER("🎩");

    companion object {
        fun of(id: String?): Voice = entries.firstOrNull { it.name == id } ?: FRIEND
    }
}

/** How big a win felt. The praise, the confetti and the stone in the jar grow with it. */
enum class WinSize(val value: Int) {
    TINY(1), SOLID(2), HUGE(3);

    companion object {
        fun of(v: Int): WinSize = entries.firstOrNull { it.value == v } ?: SOLID
    }
}

enum class Category(val id: String) {
    HOME("home"), BODY("body"), WORK("work"), MIND("mind"), PEOPLE("people"), BRAVE("brave"), CARE("care");

    companion object {
        fun of(id: String): Category? = entries.firstOrNull { it.id == id }
    }
}

/** How Russian text addresses the user: "ты сделал" or "ты сделала". English ignores it. */
enum class Gender { MALE, FEMALE;
    companion object {
        fun of(id: String?): Gender = entries.firstOrNull { it.name == id } ?: FEMALE
    }
}

private val genderMark = Regex("\\{([^{}|]*)\\|([^{}|]*)\\}")

/** Resolves "{сделал|сделала}" marks: the first form for [Gender.MALE], the second for [Gender.FEMALE]. */
fun gendered(text: String, gender: Gender): String =
    if ('|' !in text) text else genderMark.replace(text) { m -> m.groupValues[if (gender == Gender.MALE) 1 else 2] }

/**
 * Puts the name into a praise and resolves gender marks. Templates mark the name as "{n}, …" or "…, {n}…";
 * with no name the mark and its comma disappear and the sentence still reads well.
 */
fun personalize(template: String, name: String, gender: Gender): String {
    val g = gendered(template, gender)
    if (name.isNotBlank()) return g.replace("{n}", name.trim())
    // The name is asked for in onboarding; this only covers it being cleared somehow.
    val s = g
        .replace(Regex(",\\s*\\{n\\}"), "")
        .replace(Regex("\\{n\\}[,!]*\\s*"), "")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
    return s.replaceFirstChar { it.uppercaseChar() }
}

/** Stable pick from a list for a seed, so the same day or win always shows the same line. */
fun <T> List<T>.pick(seed: Long): T = this[Math.floorMod(seed, size.toLong()).toInt()]

package app.glow.domain

/** A family of awards with three degrees: III (bronze), II (silver), I (gold). */
enum class Order(val goals: List<Int>) {
    WINS(listOf(10, 50, 250)),
    TINY(listOf(5, 25, 100)),
    HUGE(listOf(1, 10, 30)),
    RUN(listOf(3, 14, 60)),
    EVENING(listOf(3, 14, 50)),
    KIND(listOf(1, 10, 30)),
    SOS(listOf(1, 5, 15)),
    REFRAME(listOf(1, 10, 30)),
    EARLY(listOf(1, 10, 30)),
    HOME(listOf(5, 20, 60)),
    BODY(listOf(5, 20, 60)),
    WORK(listOf(5, 20, 60)),
    MIND(listOf(5, 20, 60)),
    PEOPLE(listOf(5, 20, 60)),
    BRAVE(listOf(5, 20, 60)),
    CARE(listOf(5, 20, 60));

    val category: Category? get() = Category.entries.firstOrNull { it.name == name }

    fun value(s: Stats): Int = when (this) {
        WINS -> s.wins.size
        TINY -> s.tiny
        HUGE -> s.huge
        RUN -> s.bestRun
        EVENING -> s.evenings.size
        KIND -> s.personWords
        SOS -> s.sosCount
        REFRAME -> s.reframes
        EARLY -> s.early
        else -> s.byCategory[category] ?: 0
    }

    /** 0 when none earned yet, up to 3 for gold. */
    fun degree(s: Stats): Int = goals.count { value(s) >= it }

    /** The goal of the next degree, or null once gold is earned. */
    fun next(s: Stats): Int? = goals.getOrNull(degree(s))
}

/** One earned degree of an order. [degree] is 1 (bronze, III) to 3 (gold, I). */
data class Award(val order: Order, val degree: Int) {
    val id: String get() = "${order.name.lowercase()}_$degree"
}

object Orders {
    val all: List<Award> = Order.entries.flatMap { o -> (1..3).map { Award(o, it) } }

    fun earned(s: Stats): List<Award> = Order.entries.flatMap { o -> (1..o.degree(s)).map { Award(o, it) } }
}

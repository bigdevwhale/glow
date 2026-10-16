package app.glow.domain

import app.glow.data.Evening
import app.glow.data.KindWord
import app.glow.data.Win
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Everything counted from wins, kind words and evenings. */
class Stats(
    val wins: List<Win>,
    val kindWords: List<KindWord>,
    val evenings: List<Evening>,
    val sosCount: Int,
    val reframes: Int,
    now: Long,
    val zone: ZoneId,
) {
    val today: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    fun dayOf(at: Long): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    val winDays: Set<Long> = wins.map { dayOf(it.at).toEpochDay() }.toSet()

    val todayWins: List<Win> = wins.filter { dayOf(it.at) == today }

    val todayEvening: Evening? = evenings.firstOrNull { it.day == today.toEpochDay() }

    /**
     * Days in a row with at least one win, ending today, or yesterday when today has none yet.
     * It never "burns": a gap just starts a new run, and the best run stays.
     */
    val run: Int = run {
        var d = if (today.toEpochDay() in winDays) today else today.minusDays(1)
        var n = 0
        while (d.toEpochDay() in winDays) { n++; d = d.minusDays(1) }
        n
    }

    val bestRun: Int = run {
        var best = 0
        var cur = 0
        var prev = Long.MIN_VALUE
        winDays.sorted().forEach { d ->
            cur = if (d == prev + 1) cur + 1 else 1
            best = maxOf(best, cur)
            prev = d
        }
        best
    }

    /** True when there were wins before, but the last one is more than a day ago. */
    val returning: Boolean = run == 0 && wins.isNotEmpty()

    val tiny = wins.count { it.size == WinSize.TINY.value }
    val huge = wins.count { it.size == WinSize.HUGE.value }
    val early = wins.count { Instant.ofEpochMilli(it.at).atZone(zone).hour < 9 }
    val byCategory: Map<Category, Int> = Category.entries.associateWith { c -> wins.count { it.category == c.id } }
    val personWords = kindWords.count { it.source == KindWord.SOURCE_PERSON }

    fun winsIn(from: LocalDate, to: LocalDate): List<Win> = wins.filter { dayOf(it.at) in from..to }

    /** Monday of the week that holds [day]. */
    fun weekStart(day: LocalDate = today): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun report(weekStart: LocalDate = weekStart()): Report {
        val end = weekStart.plusDays(6)
        val list = winsIn(weekStart, end)
        // A week in progress is compared with the same days of the week before, not the whole of it.
        val elapsed = if (today in weekStart..end) today.toEpochDay() - weekStart.toEpochDay() else 6L
        val prev = winsIn(weekStart.minusDays(7), weekStart.minusDays(7 - elapsed))
        val top = list.filter { it.category.isNotEmpty() }.groupingBy { it.category }.eachCount().maxByOrNull { it.value }?.key?.let(Category::of)
        val busiest = list.groupingBy { dayOf(it.at) }.eachCount().maxByOrNull { it.value }
        return Report(
            start = weekStart,
            wins = list.size,
            previous = prev.size,
            huge = list.filter { it.size == WinSize.HUGE.value },
            top = top,
            busiest = busiest?.key,
            busiestCount = busiest?.value ?: 0,
            evenings = evenings.count { LocalDate.ofEpochDay(it.day) in weekStart..end },
            days = list.map { dayOf(it.at) }.toSet().size,
            perDay = (0..6).map { i -> list.count { dayOf(it.at) == weekStart.plusDays(i.toLong()) } },
        )
    }
}

/** One week at a glance: the "report on how great you are". */
class Report(
    val start: LocalDate,
    val wins: Int,
    val previous: Int,
    val huge: List<Win>,
    val top: Category?,
    val busiest: LocalDate?,
    val busiestCount: Int,
    val evenings: Int,
    /** Days of the week with at least one win. */
    val days: Int,
    /** Wins per day, Monday first. */
    val perDay: List<Int>,
)

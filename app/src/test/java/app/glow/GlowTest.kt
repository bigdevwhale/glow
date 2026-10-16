package app.glow

import app.glow.content.ContentEn
import app.glow.content.ContentRu
import app.glow.data.Win
import app.glow.domain.Gender
import app.glow.domain.Order
import app.glow.domain.Stats
import app.glow.domain.Voice
import app.glow.domain.gendered
import app.glow.domain.personalize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class GlowTest {
    private val zone = ZoneOffset.UTC

    private fun at(day: LocalDate, hour: Int = 12) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()

    @Test
    fun genderMarksPickTheRightForm() {
        assertEquals("Ты сделал это", gendered("Ты {сделал|сделала} это", Gender.MALE))
        assertEquals("Ты сделала это", gendered("Ты {сделал|сделала} это", Gender.FEMALE))
        assertEquals("готова", gendered("готов{|а}", Gender.FEMALE))
        assertEquals("готов", gendered("готов{|а}", Gender.MALE))
    }

    @Test
    fun nameIsInsertedOrDroppedCleanly() {
        assertEquals("Anna, you did it.", personalize("{n}, you did it.", "Anna", Gender.FEMALE))
        assertEquals("You did it.", personalize("{n}, you did it.", "", Gender.FEMALE))
        assertEquals("Well done.", personalize("Well done, {n}.", "", Gender.FEMALE))
        assertEquals("Well done!", personalize("Well done, {n}!", "", Gender.FEMALE))
        assertEquals("Wait. {n}!!! wow".let { personalize(it, "", Gender.MALE) }, "Wait. wow")
    }

    @Test
    fun everyLineResolvesWithoutLeftoverMarks() {
        listOf(ContentEn, ContentRu).forEach { c ->
            Voice.entries.forEach { v ->
                val t = c.voice(v)
                (t.daily + t.tiny + t.solid + t.huge + t.sos + t.evening + t.welcomeBack).forEach { line ->
                    Gender.entries.forEach { g ->
                        listOf("", "Аня").forEach { name ->
                            val out = personalize(line, name, g)
                            assertFalse("$line -> $out", out.contains('{') || out.contains('}') || out.contains('|'))
                            assertTrue(out.isNotBlank())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun translatorSpotsTraps() {
        assertEquals("absolute", ContentEn.traps("I always mess everything up").first().id)
        assertTrue(ContentRu.traps("Я опять всё испортила, я такая дура").any { it.id == "label" })
        assertTrue(ContentRu.traps("Сегодня солнечно").isEmpty())
    }

    @Test
    fun runCountsDaysInARowAndForgivesToday() {
        val today = LocalDate.of(2026, 10, 16)
        val wins = listOf(3L, 2L, 1L).map { Win(at = at(today.minusDays(it)), text = "x", size = 1) }
        val s = Stats(wins, emptyList(), emptyList(), 0, 0, at(today, 9), zone)
        assertEquals(3, s.run)
        assertEquals(3, s.bestRun)
        val gap = Stats(wins.take(1), emptyList(), emptyList(), 0, 0, at(today, 9), zone)
        assertEquals(0, gap.run)
        assertTrue(gap.returning)
    }

    @Test
    fun ordersEarnDegrees() {
        val today = LocalDate.of(2026, 10, 16)
        val wins = (0 until 12).map { Win(at = at(today.minusDays(it.toLong()), 8), text = "x", size = 3, category = "home") }
        val s = Stats(wins, emptyList(), emptyList(), 0, 0, at(today, 20), zone)
        assertEquals(1, Order.WINS.degree(s))
        assertEquals(2, Order.HUGE.degree(s))
        assertEquals(2, Order.EARLY.degree(s))
        assertEquals(1, Order.HOME.degree(s))
        assertEquals(50, Order.WINS.next(s))
    }
}

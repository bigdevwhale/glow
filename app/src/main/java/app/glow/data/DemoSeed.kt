package app.glow.data

import app.glow.content.Content
import app.glow.domain.Gender
import app.glow.domain.Orders
import app.glow.domain.Voice
import app.glow.domain.WinSize
import app.glow.domain.personalize
import app.glow.domain.pick
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Made-up wins, kind words and evenings for README screenshots; only with `-Pdemo` (app.glow.demo). */
object DemoSeed {
    private class W(val en: String, val ru: String, val cat: String, val size: Int)

    private val wins = listOf(
        W("Went for a morning run", "Пробежка утром", "body", 2),
        W("Washed all the dishes", "Перемыла всю посуду", "home", 1),
        W("Finished the quarterly report", "Закончила квартальный отчёт", "work", 3),
        W("Called mum", "Позвонила маме", "people", 1),
        W("Said no to an extra project", "Отказалась от лишнего проекта", "brave", 2),
        W("Read 30 pages", "Прочитала 30 страниц", "mind", 1),
        W("Took a long bath", "Приняла долгую ванну", "care", 1),
        W("Drank enough water", "Выпила достаточно воды", "body", 1),
        W("Fixed the leaking tap myself", "Сама починила кран", "home", 2),
        W("Presented at the team meeting", "Выступила на планёрке", "work", 2),
        W("Helped a friend move", "Помогла подруге с переездом", "people", 2),
        W("Booked the dentist at last", "Наконец записалась к стоматологу", "brave", 2),
        W("Finished an online course", "Закончила онлайн-курс", "mind", 3),
        W("Went to bed on time", "Легла спать вовремя", "care", 1),
        W("Cooked a proper dinner", "Приготовила нормальный ужин", "home", 1),
        W("Stretched for 10 minutes", "Растяжка 10 минут", "body", 1),
        W("Answered all the emails", "Разобрала все письма", "work", 1),
        W("Got out of bed on a hard day", "Встала с кровати в тяжёлый день", "", 1),
        W("Asked for help", "Попросила о помощи", "brave", 1),
        W("Practised Spanish", "Позанималась испанским", "mind", 1),
        W("Ran my first 10 km", "Пробежала первые 10 км", "body", 3),
        W("Took a lunch break, no screens", "Пообедала без телефона", "care", 1),
    )

    suspend fun seedIfEmpty(db: GlowDatabase, settings: SettingsStore) {
        val dao = db.dao()
        if (dao.wins().isNotEmpty()) return
        val ru = Locale.getDefault().language == "ru"
        val content = Content.of(Locale.getDefault())
        val name = if (ru) "Аня" else "Anna"
        val voice = Voice.FRIEND
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()

        // Seven weeks back, a few wins a day with two quiet gaps; today has two so far.
        var k = 0
        for (d in 48 downTo 0) {
            if (d == 30 || d == 31 || d == 17) continue
            val n = if (d == 0) 2 else 1 + (d * 7 + 3) % 4
            repeat(n) { i ->
                val w = wins[(k++ * 5 + d) % wins.size]
                val at = today.minusDays(d.toLong()).atTime(8 + i * 4 + d % 3, (d * 13 + i * 17) % 60).atZone(zone).toInstant().toEpochMilli()
                if (at > now) return@repeat
                val size = WinSize.of(w.size)
                val praise = personalize(content.voice(voice).forWin(size).pick(k.toLong()), name, Gender.FEMALE)
                dao.insertWin(Win(at = at, text = if (ru) w.ru else w.en, size = w.size, category = w.cat, praise = praise, voice = voice.name))
            }
        }

        val kind = listOf(
            Triple("You explained it so clearly, the whole team finally got it. Thank you!", "Ты так понятно объяснила, что вся команда наконец поняла. Спасибо!", "Max" to "Макс"),
            Triple("Honestly the kindest person I know.", "Честно, самый добрый человек, которого я знаю.", "Lena" to "Лена"),
            Triple("Your cake was the best thing at the party 🎂", "Твой торт был лучшим на празднике 🎂", "Grandma" to "Бабушка"),
            Triple("Thanks for staying late to help, it saved my week.", "Спасибо, что задержалась и помогла, ты спасла мне неделю.", "Oleg" to "Олег"),
            Triple("I'm proud of how you handled that conversation.", "Горжусь тем, как ты провела этот разговор.", "Mum" to "Мама"),
        )
        kind.forEachIndexed { i, (en, r, from) ->
            dao.insertKindWord(KindWord(at = now - (i * 6 + 2) * 86_400_000L, text = if (ru) r else en, from = if (ru) from.second else from.first))
        }
        dao.insertKindWord(KindWord(at = now - 3 * 86_400_000L, text = personalize(content.daily(voice, 4), name, Gender.FEMALE), from = voice.name, source = KindWord.SOURCE_GLOW))

        val evenings = listOf(
            Triple("Finished the report", "Stayed calm in a tense call", "My stubbornness"),
            Triple("A long walk", "A hard conversation", "That I asked for help"),
            Triple("Cooked dinner", "Didn't skip my run", "My sense of humour"),
        )
        val evRu = listOf(
            Triple("Закончила отчёт", "Сохранила спокойствие на сложном созвоне", "Своё упрямство"),
            Triple("Долгая прогулка", "Трудный разговор", "Что попросила о помощи"),
            Triple("Приготовила ужин", "Не пропустила пробежку", "Своё чувство юмора"),
        )
        for (d in 1..22) {
            if (d % 4 == 3) continue
            val (a, b, c) = (if (ru) evRu else evenings)[d % 3]
            val day = today.minusDays(d.toLong())
            dao.setEvening(Evening(day.toEpochDay(), a, b, c, day.atTime(22, 10).atZone(zone).toInstant().toEpochMilli()))
        }

        settings.update {
            it.copy(
                onboarded = true, name = name, gender = Gender.FEMALE, voice = voice, sosCount = 3, reframes = 7,
                // Everything already earned counts as celebrated, so screenshots open without a dialog.
                seenBadges = Orders.all.map { a -> a.id }.toSet(),
            )
        }
    }
}

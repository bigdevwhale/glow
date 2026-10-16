package app.glow.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.glow.domain.Gender
import app.glow.domain.Voice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    /** Used in praise and greetings; empty is fine. */
    val name: String = "",
    /** Picks the right word forms in Russian. */
    val gender: Gender = Gender.FEMALE,
    val voice: Voice = Voice.FRIEND,
    /** The praise of the day as a notification. */
    val morningOn: Boolean = true,
    /** Minutes after midnight. */
    val morningAt: Int = 9 * 60,
    /** A nudge for the three things you're proud of. */
    val eveningOn: Boolean = true,
    val eveningAt: Int = 21 * 60 + 30,
    /** "Your weekly report is ready" on Sunday evening. */
    val weeklyOn: Boolean = true,
    val haptics: Boolean = true,
    /** Badge ids already celebrated, so each unlock is shown once. */
    val seenBadges: Set<String> = emptySet(),
    /** Times the SOS screen ended with "I feel better". */
    val sosCount: Int = 0,
    /** Self-critical thoughts run through the translator. */
    val reframes: Int = 0,
    /** Added to the day so "another one" moves through the praise of the day, here and in the widget. */
    val praiseShift: Int = 0,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        gender = Gender.of(this[GENDER]),
        voice = Voice.of(this[VOICE]),
        morningOn = this[MORNING_ON] ?: true,
        morningAt = this[MORNING_AT] ?: (9 * 60),
        eveningOn = this[EVENING_ON] ?: true,
        eveningAt = this[EVENING_AT] ?: (21 * 60 + 30),
        weeklyOn = this[WEEKLY_ON] ?: true,
        haptics = this[HAPTICS] ?: true,
        seenBadges = this[SEEN_BADGES] ?: emptySet(),
        sosCount = this[SOS] ?: 0,
        reframes = this[REFRAMES] ?: 0,
        praiseShift = this[SHIFT] ?: 0,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[GENDER] = s.gender.name
            it[VOICE] = s.voice.name
            it[MORNING_ON] = s.morningOn
            it[MORNING_AT] = s.morningAt
            it[EVENING_ON] = s.eveningOn
            it[EVENING_AT] = s.eveningAt
            it[WEEKLY_ON] = s.weeklyOn
            it[HAPTICS] = s.haptics
            it[SEEN_BADGES] = s.seenBadges
            it[SOS] = s.sosCount
            it[REFRAMES] = s.reframes
            it[SHIFT] = s.praiseShift
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val GENDER = stringPreferencesKey("gender")
        val VOICE = stringPreferencesKey("voice")
        val MORNING_ON = booleanPreferencesKey("morning_on")
        val MORNING_AT = intPreferencesKey("morning_at")
        val EVENING_ON = booleanPreferencesKey("evening_on")
        val EVENING_AT = intPreferencesKey("evening_at")
        val WEEKLY_ON = booleanPreferencesKey("weekly_on")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SEEN_BADGES = stringSetPreferencesKey("seen_badges")
        val SOS = intPreferencesKey("sos_count")
        val REFRAMES = intPreferencesKey("reframes")
        val SHIFT = intPreferencesKey("praise_shift")
    }
}

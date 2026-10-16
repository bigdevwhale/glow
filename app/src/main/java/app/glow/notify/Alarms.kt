package app.glow.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.glow.MainActivity
import app.glow.R
import app.glow.container
import app.glow.content.Content
import app.glow.domain.Stats
import app.glow.domain.gendered
import app.glow.domain.personalize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** The three moments Glow comes by on its own: morning praise, the evening of pride, the Sunday report. */
object Alarms {
    private const val CHANNEL = "praise_v1"
    const val EXTRA_ROUTE = "route"

    private const val ACTION_MORNING = "app.glow.notify.MORNING"
    private const val ACTION_EVENING = "app.glow.notify.EVENING"
    private const val ACTION_WEEKLY = "app.glow.notify.WEEKLY"

    private const val ID_MORNING = 1
    private const val ID_EVENING = 2
    private const val ID_WEEKLY = 3

    /** Sunday, 19:00. */
    private const val WEEKLY_AT = 19 * 60

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, context.getString(R.string.channel_praise), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_praise_desc)
                }
            )
        }
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Next Sunday at [minutes], strictly after now. */
    fun nextSunday(minutes: Int, now: LocalDateTime): LocalDateTime {
        var d = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(minutes / 60, minutes % 60)
        if (!d.isAfter(now)) d = d.plusWeeks(1)
        return d
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        fun set(action: String, code: Int, at: LocalDateTime?) {
            val p = pi(context, action, code)
            // Reminders don't need to-the-second precision; this avoids the exact-alarm permission.
            if (at == null || !s.onboarded) am.cancel(p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.atZone(zone).toInstant().toEpochMilli(), p)
        }
        set(ACTION_MORNING, 1, if (s.morningOn) nextAt(s.morningAt, now) else null)
        set(ACTION_EVENING, 2, if (s.eveningOn) nextAt(s.eveningAt, now) else null)
        set(ACTION_WEEKLY, 3, if (s.weeklyOn) nextSunday(WEEKLY_AT, now) else null)
    }

    suspend fun fire(context: Context, action: String) {
        val c = context.container
        val s = c.settings.now()
        val content = Content.of(context.resources.configuration.locales[0])
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        when (action) {
            ACTION_MORNING -> if (s.morningOn) {
                val text = personalize(content.daily(s.voice, today.toEpochDay() + s.praiseShift), s.name, s.gender)
                notify(context, ID_MORNING, s.voice.emoji + " " + content.voice(s.voice).name, text, "today")
            }
            ACTION_EVENING -> if (s.eveningOn && c.db.dao().evening(today.toEpochDay()) == null) {
                notify(context, ID_EVENING, context.getString(R.string.notif_evening_title), context.getString(R.string.notif_evening_text), "evening")
            }
            ACTION_WEEKLY -> if (s.weeklyOn) {
                val dao = c.db.dao()
                val stats = Stats(dao.wins(), dao.kindWords(), dao.evenings(), s.sosCount, s.reframes, System.currentTimeMillis(), zone)
                val n = stats.report().wins
                val wins = context.resources.getQuantityString(R.plurals.wins, n, n)
                notify(context, ID_WEEKLY, context.getString(R.string.notif_weekly_title), gendered(context.getString(R.string.notif_weekly_text, wins), s.gender), "report")
            }
        }
    }

    private fun open(context: Context, code: Int, route: String) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, id: Int, title: String, text: String, route: String) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, 100 + id, route))
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    internal val actions = setOf(ACTION_MORNING, ACTION_EVENING, ACTION_WEEKLY)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in Alarms.actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context, action)
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                context.container.changed()
            } finally {
                pending.finish()
            }
        }
    }
}

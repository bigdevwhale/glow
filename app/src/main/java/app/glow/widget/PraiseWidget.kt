package app.glow.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.glow.MainActivity
import app.glow.R
import app.glow.container
import app.glow.content.Content
import app.glow.domain.personalize
import app.glow.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The praise of the day on the home screen; the arrow shows another one, a tap opens the app. */
class PraiseWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NEXT) return super.onReceive(context, intent)
        val pending = goAsync()
        scope.launch {
            try {
                context.container.settings.update { it.copy(praiseShift = it.praiseShift + 1) }
                refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_NEXT = "app.glow.widget.NEXT"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PraiseWidget::class.java))
            if (ids.isEmpty()) return
            val s = context.container.settings.now()
            val content = Content.of(context.resources.configuration.locales[0])
            val day = LocalDate.now().toEpochDay()
            val views = RemoteViews(context.packageName, R.layout.widget_praise)
            views.setTextViewText(R.id.label, s.voice.emoji + "  " + content.voice(s.voice).name)
            views.setTextViewText(R.id.text, personalize(content.daily(s.voice, day + s.praiseShift), s.name, s.gender))
            views.setOnClickPendingIntent(
                R.id.root,
                PendingIntent.getActivity(
                    context, 5000,
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Alarms.EXTRA_ROUTE, "today"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            views.setOnClickPendingIntent(
                R.id.next,
                PendingIntent.getBroadcast(
                    context, 5001,
                    Intent(context, PraiseWidget::class.java).setAction(ACTION_NEXT),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            manager.updateAppWidget(ids, views)
        }
    }
}

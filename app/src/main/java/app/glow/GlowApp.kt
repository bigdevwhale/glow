package app.glow

import android.app.Application
import android.content.Context
import app.glow.data.DemoSeed
import app.glow.data.GlowDatabase
import app.glow.data.SettingsStore
import app.glow.notify.Alarms
import app.glow.widget.PraiseWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppContainer(private val context: Context) {
    val db = GlowDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Screenshots attached to kind words. */
    val images: File get() = File(context.filesDir, "kind").apply { mkdirs() }

    /** Fire-and-forget so a write survives the screen being closed right after tapping save. */
    fun write(block: suspend GlowDatabase.() -> Unit) {
        scope.launch {
            db.block()
            changed()
        }
    }

    /** Re-arm reminders and redraw the home-screen widget. */
    suspend fun changed() {
        Alarms.reschedule(context)
        PraiseWidget.refresh(context)
    }
}

class GlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container.db, container.settings)
            container.changed()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as GlowApp).container

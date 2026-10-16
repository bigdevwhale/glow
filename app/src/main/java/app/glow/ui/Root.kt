package app.glow.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.glow.BuildConfig
import app.glow.R
import app.glow.container
import app.glow.data.Evening
import app.glow.data.KindWord
import app.glow.data.Settings
import app.glow.data.Win
import app.glow.domain.Award
import app.glow.domain.Order
import app.glow.domain.Orders
import app.glow.domain.Stats
import app.glow.notify.Alarms
import app.glow.ui.components.CelebrateDialog
import app.glow.ui.components.rememberNow
import app.glow.ui.jar.JarTab
import app.glow.ui.kind.EditKindScreen
import app.glow.ui.kind.KindTab
import app.glow.ui.kind.ShuffleScreen
import app.glow.ui.me.CertificateScreen
import app.glow.ui.me.DiaryScreen
import app.glow.ui.me.MeTab
import app.glow.ui.me.ReportScreen
import app.glow.ui.me.VoiceScreen
import app.glow.ui.onboarding.OnboardingScreen
import app.glow.ui.settings.SettingsScreen
import app.glow.ui.theme.Inter
import app.glow.ui.theme.U
import app.glow.ui.today.AddWinScreen
import app.glow.ui.today.EveningScreen
import app.glow.ui.today.PraiseScreen
import app.glow.ui.today.SosScreen
import app.glow.ui.today.TodayTab
import app.glow.ui.today.TranslatorScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId

enum class Tab { TODAY, JAR, KIND, ME }

/** Everything stored, plus what is derived from it, recomputed once per minute or change. */
@Immutable
class GlowData(
    val wins: List<Win>,
    val kindWords: List<KindWord>,
    val evenings: List<Evening>,
    val settings: Settings,
    val now: Long,
) {
    val stats = Stats(wins, kindWords, evenings, settings.sosCount, settings.reframes, now, ZoneId.systemDefault())
    val earned: List<Award> = Orders.earned(stats)
}

/** Everything a tab can open. */
class Go(private val nav: NavHostController) {
    fun addWin(category: String = "") = nav.navigate("add?id=0&cat=$category")
    fun editWin(id: Long) = nav.navigate("add?id=$id&cat=")
    fun praise(id: Long) = nav.navigate("praise/$id") { popUpTo("main") }
    fun evening() = nav.navigate("evening")
    fun sos() = nav.navigate("sos")
    fun translator() = nav.navigate("translator")
    fun newKind() = nav.navigate("kind/0")
    fun kind(id: Long) = nav.navigate("kind/$id")
    fun shuffle() = nav.navigate("shuffle")
    fun report() = nav.navigate("report")
    fun voice() = nav.navigate("voice")
    fun diary() = nav.navigate("diary")
    fun certificate(a: Award) = nav.navigate("certificate/${a.order.name}/${a.degree}")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
    fun home() = nav.popBackStack("main", inclusive = false)
}

@Composable
fun GlowRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.db.dao()
    val wins by dao.observeWins().collectAsStateWithLifecycle(initialValue = null)
    val kindWords by dao.observeKindWords().collectAsStateWithLifecycle(initialValue = emptyList())
    val evenings by dao.observeEvenings().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(60_000)
    val data = remember(wins, kindWords, evenings, settings, minute) {
        GlowData(wins.orEmpty(), kindWords, evenings, settings, System.currentTimeMillis())
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded, wins != null) {
        val route = requested ?: return@LaunchedEffect
        if (!settings.onboarded || wins == null) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        tab = Tab.TODAY
        when (route) {
            "evening" -> go.evening()
            "report" -> go.report()
        }
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalPerson provides Person(settings.name, settings.gender)) {
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.background(U.c.bg),
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("welcome") {
                OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
            }
            composable("main") { MainTabs(tab, { tab = it }, go, data) }
            composable(
                "add?id={id}&cat={cat}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("cat") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { e ->
                AddWinScreen(e.arguments?.getLong("id") ?: 0L, e.arguments?.getString("cat").orEmpty(), data, go)
            }
            composable("praise/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                PraiseScreen(e.arguments?.getLong("id") ?: 0L, data, go)
            }
            composable("evening") { EveningScreen(data, onBack = { go.back() }) }
            composable("sos") { SosScreen(data, onClose = { go.back() }) }
            composable("translator") { TranslatorScreen(data, onBack = { go.back() }) }
            composable("kind/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                EditKindScreen(e.arguments?.getLong("id") ?: 0L, data, onDone = { go.back() })
            }
            composable("shuffle") { ShuffleScreen(data, onClose = { go.back() }) }
            composable("report") { ReportScreen(data, onBack = { go.back() }) }
            composable("voice") { VoiceScreen(data, onBack = { go.back() }) }
            composable("diary") { DiaryScreen(data, onBack = { go.back() }) }
            composable(
                "certificate/{order}/{degree}",
                arguments = listOf(navArgument("order") { type = NavType.StringType }, navArgument("degree") { type = NavType.IntType }),
            ) { e ->
                val order = Order.entries.firstOrNull { it.name == e.arguments?.getString("order") } ?: Order.WINS
                CertificateScreen(Award(order, e.arguments?.getInt("degree") ?: 1), data, onBack = { go.back() })
            }
            composable("settings") { SettingsScreen(settings, onBack = { go.back() }) }
        }

        // New orders wait until the user is back on the tabs, so they never cut into a praise.
        val entry by nav.currentBackStackEntryAsState()
        if (settings.onboarded && wins != null && entry?.destination?.route == "main") Celebrations(data, go)
    }
}

/** New orders, each celebrated once. */
@Composable
private fun Celebrations(data: GlowData, go: Go) {
    val container = LocalContext.current.container
    val fresh = data.earned.filter { it.id !in data.settings.seenBadges }
    if (fresh.isEmpty()) return
    val a = fresh.last()
    fun seen() = container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + fresh.map { f -> f.id }) } }
    CelebrateDialog(
        award = a,
        eyebrow = stringResource(R.string.order_new),
        title = awardTitle(a),
        text = stringResource(R.string.cert_for, orderRequirement(a.order, a.order.goals[a.degree - 1])) +
            if (fresh.size > 1) "\n" + stringResource(R.string.order_more, fresh.size - 1) else "",
        action = stringResource(R.string.certificate),
        onAction = { seen(); go.certificate(a) },
    ) { seen() }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: GlowData) {
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.TODAY -> TodayTab(data, go)
                Tab.JAR -> JarTab(data, go)
                Tab.KIND -> KindTab(data, go)
                Tab.ME -> MeTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.WbSunny, stringResource(R.string.tab_today), tab == Tab.TODAY) { onTab(Tab.TODAY) }
            NavItem(Icons.Rounded.AutoAwesome, stringResource(R.string.tab_jar), tab == Tab.JAR) { onTab(Tab.JAR) }
            NavItem(Icons.Rounded.Favorite, stringResource(R.string.tab_kind), tab == Tab.KIND) { onTab(Tab.KIND) }
            NavItem(Icons.Rounded.Person, stringResource(R.string.tab_me), tab == Tab.ME) { onTab(Tab.ME) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Inter, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}

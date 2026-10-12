package pw.shitcode.spent.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pw.shitcode.spent.R
import pw.shitcode.spent.ui.components.DockItem
import pw.shitcode.spent.ui.components.Grain
import pw.shitcode.spent.ui.components.IconId
import pw.shitcode.spent.ui.components.SpentDock
import pw.shitcode.spent.ui.detail.AppDetailScreen
import pw.shitcode.spent.ui.detail.AppDetailViewModel
import pw.shitcode.spent.ui.home.HomeScreen
import pw.shitcode.spent.ui.home.HomeViewModel
import pw.shitcode.spent.ui.notifications.NotifViewModel
import pw.shitcode.spent.ui.notifications.NotificationsScreen
import pw.shitcode.spent.ui.settings.SettingsScreen
import pw.shitcode.spent.ui.settings.SettingsViewModel
import pw.shitcode.spent.ui.stats.StatsScreen
import pw.shitcode.spent.ui.stats.StatsViewModel
import pw.shitcode.spent.ui.theme.Accent
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.ui.theme.ThemeMode
import pw.shitcode.spent.util.rememberReducedMotion

enum class Tab { Today, Stats, Notifications, Settings }

@Composable
fun SpentApp(
    vm: HomeViewModel,
    stats: StatsViewModel,
    notif: NotifViewModel,
    detail: AppDetailViewModel,
    settings: SettingsViewModel,
    mode: ThemeMode,
    accent: Accent,
) {
    val c = Spent.colors
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        val reduced = rememberReducedMotion()
        val offsetPx = with(LocalDensity.current) { 10.dp.roundToPx() }
        val openPkg = detail.pkg
        BackHandler(enabled = openPkg != null) { detail.close() }
        if (openPkg != null) {
            AppDetailScreen(detail, vm.notifAccess, onBack = detail::close)
        } else AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (reduced) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    (fadeIn(tween(300, delayMillis = 60)) +
                        slideInVertically(tween(350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) { offsetPx }) togetherWith
                        fadeOut(tween(120))
                }
            },
            label = "tab",
        ) { current ->
            when (current) {
                Tab.Today -> HomeScreen(vm, onOpenNotifications = { tab = Tab.Notifications }, onOpenApp = detail::open)
                Tab.Stats -> StatsScreen(stats, vm.usageAccess, onOpenApp = detail::open)
                Tab.Notifications -> NotificationsScreen(notif, vm.notifAccess, onOpenApp = detail::open)
                Tab.Settings -> SettingsScreen(
                    mode = mode,
                    onMode = settings::setMode,
                    accent = accent,
                    onAccent = settings::setAccent,
                    usageAccess = vm.usageAccess,
                    notifAccess = vm.notifAccess,
                    exportStatus = settings.exportStatus,
                    onExport = settings::export,
                )
            }
        }
        Grain()
        SpentDock(
            items = listOf(
                DockItem(IconId.Home, stringResource(R.string.tab_today)),
                DockItem(IconId.Stats, stringResource(R.string.tab_stats)),
                DockItem(IconId.Bell, stringResource(R.string.tab_notifications)),
                DockItem(IconId.Sliders, stringResource(R.string.tab_settings)),
            ),
            selected = tab.ordinal,
            onSelect = {
                detail.close()
                tab = Tab.entries[it]
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}


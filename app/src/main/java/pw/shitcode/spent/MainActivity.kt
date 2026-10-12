package pw.shitcode.spent

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import pw.shitcode.spent.ui.SpentApp
import pw.shitcode.spent.ui.detail.AppDetailViewModel
import pw.shitcode.spent.ui.home.HomeViewModel
import pw.shitcode.spent.ui.notifications.NotifViewModel
import pw.shitcode.spent.ui.settings.SettingsViewModel
import pw.shitcode.spent.ui.stats.StatsViewModel
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.ui.theme.SpentTheme
import pw.shitcode.spent.work.SyncScheduler

class MainActivity : AppCompatActivity() {
    private val vm: HomeViewModel by viewModels()
    private val statsVm: StatsViewModel by viewModels()
    private val notifVm: NotifViewModel by viewModels()
    private val detailVm: AppDetailViewModel by viewModels()
    private val settingsVm: SettingsViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        vm.refresh()
        statsVm.refresh()
        notifVm.refresh()
        detailVm.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncScheduler.schedule(applicationContext)
        setContent {
            // Пока DataStore не прочитан, ничего не рисуем: так не будет вспышки не той темы
            val prefs = settingsVm.prefs ?: return@setContent

            SpentTheme(mode = prefs.mode, accent = prefs.accent) {
                val dark = Spent.colors.dark
                DisposableEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                SpentApp(
                    vm = vm,
                    stats = statsVm,
                    notif = notifVm,
                    detail = detailVm,
                    settings = settingsVm,
                    mode = prefs.mode,
                    accent = prefs.accent,
                )
            }
        }
    }
}

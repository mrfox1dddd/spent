package pw.shitcode.spent.ui.notifications

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pw.shitcode.spent.data.usage.AppInfoLoader
import pw.shitcode.spent.data.usage.NotifSnapshot
import pw.shitcode.spent.data.usage.StatsRange
import pw.shitcode.spent.data.usage.UsageRepository
import pw.shitcode.spent.ui.home.TopApp
import pw.shitcode.spent.util.Access

/** apps: TopApp, где millis хранит число уведомлений. */
class NotifData(val snapshot: NotifSnapshot, val apps: List<TopApp>)

class NotifViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UsageRepository(app)
    private val loader = AppInfoLoader(app)
    private var job: Job? = null

    var range by mutableStateOf(StatsRange.Week)
        private set
    var data by mutableStateOf<NotifData?>(null)
        private set

    fun selectRange(value: StatsRange) {
        if (value == range) return
        range = value
        load()
    }

    /** При возврате в приложение: перечитывает текущий период. */
    fun refresh() = load()

    private fun load() {
        val ctx = getApplication<Application>()
        if (!Access.hasNotifications(ctx)) {
            data = null
            return
        }
        job?.cancel()
        val wanted = range
        job = viewModelScope.launch(Dispatchers.IO) {
            val s = repo.notifStats(wanted, appLimit = 50)
            data = NotifData(
                snapshot = s,
                apps = s.apps.map {
                    val info = loader.load(it.pkg)
                    TopApp(it.pkg, info.label, info.icon, it.count.toLong())
                },
            )
        }
    }
}

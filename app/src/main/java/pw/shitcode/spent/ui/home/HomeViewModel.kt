package pw.shitcode.spent.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pw.shitcode.spent.data.usage.AppInfoLoader
import pw.shitcode.spent.data.usage.UsageRepository
import pw.shitcode.spent.util.Access

class TopApp(val pkg: String, val label: String, val icon: ImageBitmap?, val millis: Long)

class HomeData(
    val total: Long,
    val hours: LongArray,
    val yesterday: Long,
    val weekAvg: Long,
    val top: List<TopApp>,
    val notifToday: Int,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UsageRepository(app)
    private val loader = AppInfoLoader(app)
    private var job: Job? = null

    var usageAccess by mutableStateOf(Access.hasUsage(app))
        private set
    var notifAccess by mutableStateOf(Access.hasNotifications(app))
        private set
    var home by mutableStateOf<HomeData?>(null)
        private set

    /** Вызывать при возврате в приложение: перепроверяет доступы, синхронизирует и обновляет данные. */
    fun refresh() {
        val ctx = getApplication<Application>()
        usageAccess = Access.hasUsage(ctx)
        notifAccess = Access.hasNotifications(ctx)
        if (!usageAccess) {
            home = null
            return
        }
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            repo.sync()
            val s = repo.home(topLimit = 5)
            home = HomeData(
                total = s.total,
                hours = s.hours,
                yesterday = s.yesterday,
                weekAvg = s.weekAvg,
                top = s.top.map {
                    val info = loader.load(it.pkg)
                    TopApp(it.pkg, info.label, info.icon, it.millis)
                },
                notifToday = s.notifToday,
            )
        }
    }
}

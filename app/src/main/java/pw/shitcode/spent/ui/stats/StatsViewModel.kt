package pw.shitcode.spent.ui.stats

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
import pw.shitcode.spent.data.usage.StatsRange
import pw.shitcode.spent.data.usage.StatsSnapshot
import pw.shitcode.spent.data.usage.UsageRepository
import pw.shitcode.spent.ui.home.TopApp
import pw.shitcode.spent.util.Access

class StatsData(val snapshot: StatsSnapshot, val apps: List<TopApp>) {
    val range get() = snapshot.range
}

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UsageRepository(app)
    private val loader = AppInfoLoader(app)
    private var job: Job? = null

    var range by mutableStateOf(StatsRange.Week)
        private set
    var data by mutableStateOf<StatsData?>(null)
        private set

    fun selectRange(value: StatsRange) {
        if (value == range) return
        range = value
        load(sync = false)
    }

    /** При возврате в приложение: синхронизирует (если давно не было) и перечитывает текущий период. */
    fun refresh() = load(sync = true)

    private fun load(sync: Boolean) {
        val ctx = getApplication<Application>()
        if (!Access.hasUsage(ctx)) {
            data = null
            return
        }
        job?.cancel()
        val wanted = range
        job = viewModelScope.launch(Dispatchers.IO) {
            if (sync) repo.sync()
            val s = repo.stats(wanted, appLimit = 50)
            data = StatsData(
                snapshot = s,
                apps = s.apps.map {
                    val info = loader.load(it.pkg)
                    TopApp(it.pkg, info.label, info.icon, it.millis)
                },
            )
        }
    }
}

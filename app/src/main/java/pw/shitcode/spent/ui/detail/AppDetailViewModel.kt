package pw.shitcode.spent.ui.detail

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
import pw.shitcode.spent.data.usage.AppSnapshot
import pw.shitcode.spent.data.usage.StatsRange
import pw.shitcode.spent.data.usage.UsageRepository
import pw.shitcode.spent.util.Access

class AppDetailData(val pkg: String, val label: String, val icon: ImageBitmap?, val snapshot: AppSnapshot)

class AppDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = UsageRepository(app)
    private val loader = AppInfoLoader(app)
    private var job: Job? = null

    /** Открытое приложение, null когда показаны обычные вкладки. */
    var pkg by mutableStateOf<String?>(null)
        private set
    var range by mutableStateOf(StatsRange.Week)
        private set
    var data by mutableStateOf<AppDetailData?>(null)
        private set

    fun open(value: String) {
        if (pkg != value) data = null
        pkg = value
        load()
    }

    fun close() {
        job?.cancel()
        pkg = null
        data = null
    }

    fun selectRange(value: StatsRange) {
        if (value == range) return
        range = value
        load()
    }

    /** При возврате в приложение: перечитывает данные открытого приложения. */
    fun refresh() = load()

    private fun load() {
        val target = pkg ?: return
        if (!Access.hasUsage(getApplication<Application>())) return
        job?.cancel()
        val wanted = range
        job = viewModelScope.launch(Dispatchers.IO) {
            val snapshot = repo.appStats(target, wanted)
            val info = loader.load(target)
            data = AppDetailData(target, info.label, info.icon, snapshot)
        }
    }
}

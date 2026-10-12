package pw.shitcode.spent.ui.settings

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pw.shitcode.spent.data.export.ExportFormat
import pw.shitcode.spent.data.prefs.Prefs
import pw.shitcode.spent.data.prefs.SettingsStore
import pw.shitcode.spent.data.usage.UsageRepository
import pw.shitcode.spent.ui.theme.Accent
import pw.shitcode.spent.ui.theme.ThemeMode
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

enum class ExportType { Csv, Json }

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data object Working : ExportStatus
    data class Done(val rows: Int) : ExportStatus
    data object Failed : ExportStatus
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val repo by lazy { UsageRepository(app) }

    /** null, пока DataStore не отдал первое значение: до этого момента тему не рисуем, чтобы не мигать. */
    var prefs by mutableStateOf<Prefs?>(null)
        private set

    var exportStatus by mutableStateOf<ExportStatus>(ExportStatus.Idle)
        private set

    init {
        viewModelScope.launch { store.prefs.collect { prefs = it } }
    }

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { store.setMode(mode) }
    }

    fun setAccent(accent: Accent) {
        viewModelScope.launch { store.setAccent(accent) }
    }

    /** Пишет всю историю в файл, который пользователь выбрал через системный диалог (SAF). */
    fun export(uri: Uri, type: ExportType) {
        if (exportStatus is ExportStatus.Working) return
        exportStatus = ExportStatus.Working
        viewModelScope.launch(Dispatchers.IO) {
            exportStatus = try {
                runCatching { repo.sync() } // свежие данные за сегодня, без доступа вернёт false
                val rows = repo.exportRows()
                val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                    ?: throw IOException("openOutputStream returned null")
                stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    when (type) {
                        ExportType.Csv -> ExportFormat.writeCsv(rows, writer)
                        ExportType.Json -> ExportFormat.writeJson(
                            rows,
                            writer,
                            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now().withNano(0)),
                        )
                    }
                }
                ExportStatus.Done(rows.size)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ExportStatus.Failed
            }
        }
    }
}

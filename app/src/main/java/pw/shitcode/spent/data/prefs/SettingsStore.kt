package pw.shitcode.spent.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import pw.shitcode.spent.ui.theme.Accent
import pw.shitcode.spent.ui.theme.ThemeMode
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "spent_settings")

/** Настройки оформления. Язык хранит сама система (AppCompat, autoStoreLocales). */
data class Prefs(
    val mode: ThemeMode = ThemeMode.System,
    val accent: Accent = Accent.Mint,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val prefs: Flow<Prefs> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            Prefs(
                mode = ThemeMode.entries.firstOrNull { it.name == p[KEY_MODE] } ?: ThemeMode.System,
                accent = Accent.entries.firstOrNull { it.name == p[KEY_ACCENT] } ?: Accent.Mint,
            )
        }

    suspend fun setMode(mode: ThemeMode) {
        store.edit { it[KEY_MODE] = mode.name }
    }

    suspend fun setAccent(accent: Accent) {
        store.edit { it[KEY_ACCENT] = accent.name }
    }

    private companion object {
        val KEY_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT = stringPreferencesKey("accent")
    }
}

package pw.shitcode.spent.data.usage

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

class AppInfo(val label: String, val icon: ImageBitmap?)

class AppInfoLoader(context: Context) {
    private val pm = context.applicationContext.packageManager
    private val cache = HashMap<String, AppInfo>()
    private val labels = HashMap<String, String>()

    /** Только название, без отрисовки иконки: дешевле для экспорта. Для удалённых приложений вернёт имя пакета. */
    @Synchronized
    fun label(pkg: String): String = labels.getOrPut(pkg) {
        try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    /** Вызывать не с главного потока: иконка рисуется в битмап. */
    @Synchronized
    fun load(pkg: String): AppInfo = cache.getOrPut(pkg) {
        try {
            val info = pm.getApplicationInfo(pkg, 0)
            AppInfo(
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap(),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            AppInfo(pkg, null)
        }
    }
}

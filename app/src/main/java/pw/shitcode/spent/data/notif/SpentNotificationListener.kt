package pw.shitcode.spent.data.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pw.shitcode.spent.data.db.SpentDatabase
import java.time.LocalDateTime

/** Считает новые уведомления по приложениям. Содержимое уведомлений не читается и не хранится. */
class SpentNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // обновления одного и того же уведомления (прогресс, правки) приходят повторно: помним последние
    private val seen = object : LinkedHashMap<String, Long>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) = size > 200
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        if (sbn.isOngoing) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (seen[sbn.key] == sbn.postTime) return
        seen[sbn.key] = sbn.postTime

        val pkg = sbn.packageName
        val now = LocalDateTime.now()
        val day = now.toLocalDate().toEpochDay()
        val hour = now.hour
        val db = SpentDatabase.get(applicationContext)
        scope.launch { db.countNotification(pkg, day, hour) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

package pw.shitcode.spent.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity

/** Время в приложении за конкретный час. day = LocalDate.toEpochDay(), hour = 0..23 (локальное время). */
@Entity(tableName = "hourly_usage", primaryKeys = ["pkg", "day", "hour"])
data class HourlyUsage(val pkg: String, val day: Long, val hour: Int, val millis: Long)

/**
 * Итог за день по приложению. Хранится навсегда.
 * approx = true: день восстановлен из укрупнённых системных корзин Android (дни, недели, месяцы, годы),
 * точных событий за него уже не было. Запусков для таких дней нет.
 */
@Entity(tableName = "daily_usage", primaryKeys = ["pkg", "day"])
data class DailyUsage(
    val pkg: String,
    val day: Long,
    val millis: Long,
    val launches: Int,
    @ColumnInfo(defaultValue = "0") val approx: Boolean = false,
)

/** Счётчик уведомлений по приложению и дню. */
@Entity(tableName = "notification_daily", primaryKeys = ["pkg", "day"])
data class NotificationDaily(val pkg: String, val day: Long, val count: Int)

/** Счётчик уведомлений по приложению, дню и часу (для графика за сегодня). Ведётся с версии базы 3. */
@Entity(tableName = "notification_hourly", primaryKeys = ["pkg", "day", "hour"])
data class NotificationHourly(val pkg: String, val day: Long, val hour: Int, val count: Int)

data class DayCount(val day: Long, val count: Int)
data class HourCount(val hour: Int, val count: Int)
data class AppTotal(val pkg: String, val millis: Long, val launches: Int)
data class HourTotal(val hour: Int, val millis: Long)
data class DayTotal(val day: Long, val millis: Long)
data class NotifTotal(val pkg: String, val count: Int)

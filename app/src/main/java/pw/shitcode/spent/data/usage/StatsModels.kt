package pw.shitcode.spent.data.usage

import pw.shitcode.spent.data.db.AppTotal
import pw.shitcode.spent.data.db.NotifTotal
import java.time.LocalDate

enum class StatsRange { Day, Week, Month, Year, All }

/**
 * Статистика одного приложения. Столбики и approx как в StatsSnapshot.
 * launches: только по точным дням (для приблизительных запусков нет). notifications: считаются с выдачи доступа.
 */
class AppSnapshot(
    val range: StatsRange,
    val values: List<Long>,
    val dates: List<LocalDate>,
    val approx: List<Boolean>,
    val total: Long,
    val days: Int,
    val launches: Int,
    val notifications: Int,
    val exactFrom: LocalDate?,
)

/**
 * То же, что StatsSnapshot, но для уведомлений. since: с какого дня идёт счёт (истории до выдачи доступа нет).
 * Day: values по часам, Week/Month: по дню, Year/All: по месяцу.
 */
class NotifSnapshot(
    val range: StatsRange,
    val values: List<Long>,
    val dates: List<LocalDate>,
    val total: Long,
    val days: Int,
    val apps: List<NotifTotal>,
    val since: LocalDate?,
)

/**
 * values: столбики графика. Day: 24 часа (dates пуст). Week/Month: по дню. Year/All: по месяцу (dates = первые числа).
 * days: сколько дней реально учтено для среднего (не раньше первого дня с данными).
 * approx: для каждого столбика, восстановлен ли он из укрупнённых системных данных (пусто для Day).
 * exactFrom: с какого дня данные точные.
 */
class StatsSnapshot(
    val range: StatsRange,
    val values: List<Long>,
    val dates: List<LocalDate>,
    val total: Long,
    val days: Int,
    val apps: List<AppTotal>,
    val approx: List<Boolean> = emptyList(),
    val exactFrom: LocalDate? = null,
)

package pw.shitcode.spent.data.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import pw.shitcode.spent.data.db.DailyUsage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Восстанавливает историю глубже, чем отдаёт queryEvents (около недели), из укрупнённых корзин Android:
 * дни, недели, месяцы, годы (queryUsageStats). Идём от мелких корзин к крупным. Крупная корзина даёт только
 * то, что не объяснено мелкими и точными данными: остаток размазывается поровну по свободным дням.
 * Итог: по каждому дню приложение получает приблизительное время, помеченное approx = true.
 * Суммы за месяц и год при этом совпадают с системными, а раскладка по дням внутри старой недели или месяца условная.
 */
class UsageArchive(private val ctx: Context) {

    private class Bucket(val pkg: String, val start: Long, val end: Long, val millis: Long)

    /**
     * existing: уже сохранённые строки (точные и приблизительные), по ним считаем покрытие.
     * exactFromDay: с этого дня данные считаются точными и не трогаются.
     */
    fun build(existing: List<DailyUsage>, exactFromDay: Long, today: LocalDate, zone: ZoneId): List<DailyUsage> {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val todayDay = today.toEpochDay()
        val winStart = todayDay - WINDOW_DAYS
        val size = (todayDay - winStart + 1).toInt()
        fun idx(day: Long) = (day - winStart).toInt()

        val covered = BooleanArray(size)
        for (d in maxOf(exactFromDay, winStart)..todayDay) covered[idx(d)] = true

        val alloc = HashMap<String, DoubleArray>()
        val existingKeys = HashSet<Pair<String, Long>>()
        for (r in existing) {
            if (r.day < winStart || r.day > todayDay) continue
            covered[idx(r.day)] = true
            alloc.getOrPut(r.pkg) { DoubleArray(size) }[idx(r.day)] += r.millis.toDouble()
            existingKeys.add(r.pkg to r.day)
        }

        val fromMs = LocalDate.ofEpochDay(winStart).atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = System.currentTimeMillis()

        for (type in LEVELS) {
            // покрытие уровня считаем по состоянию до него: все приложения видят одни и те же свободные дни
            val frozen = covered.copyOf()
            for (b in readBuckets(usm, type, fromMs, toMs, zone)) {
                val from = maxOf(b.start, winStart)
                val to = minOf(b.end, todayDay)
                if (to < from) continue
                val arr = alloc.getOrPut(b.pkg) { DoubleArray(size) }

                var allocated = 0.0
                var free = 0
                for (d in from..to) {
                    val i = idx(d)
                    allocated += arr[i]
                    if (!frozen[i]) free++
                }
                if (free == 0) continue

                val rest = b.millis - allocated
                // остаток в пределах погрешности (системные суммы чуть расходятся с событиями) не размазываем
                if (rest < MIN_REST_MS || (allocated > 0 && rest < b.millis * NOISE_SHARE)) continue
                val share = rest / free
                for (d in from..to) {
                    val i = idx(d)
                    if (!frozen[i]) {
                        arr[i] += share
                        covered[i] = true
                    }
                }
            }
        }

        val out = ArrayList<DailyUsage>()
        for ((pkg, arr) in alloc) {
            for (i in 0 until size) {
                val ms = arr[i].toLong()
                val day = winStart + i
                if (ms >= MIN_ROW_MS && (pkg to day) !in existingKeys) {
                    out.add(DailyUsage(pkg, day, ms, 0, approx = true))
                }
            }
        }
        return out
    }

    private fun readBuckets(usm: UsageStatsManager, type: Int, fromMs: Long, toMs: Long, zone: ZoneId): List<Bucket> {
        val stats = runCatching { usm.queryUsageStats(type, fromMs, toMs) }.getOrNull() ?: return emptyList()
        val merged = HashMap<Pair<String, Long>, Bucket>()
        for (u in stats) {
            val pkg = u.packageName ?: continue
            val ms = u.totalTimeInForeground
            if (ms <= 0L) continue
            // система может вернуть корзину другого размера, если запрошенной нет: такие пропускаем
            if (u.lastTimeStamp - u.firstTimeStamp > maxSpanMs(type)) continue

            // +12 часов, чтобы сдвиг часового пояса или перехода на летнее время не уводил дату на соседний день
            val start = Instant.ofEpochMilli(u.firstTimeStamp + HALF_DAY_MS).atZone(zone).toLocalDate()
            val (first, last) = when (type) {
                UsageStatsManager.INTERVAL_DAILY -> start to start
                UsageStatsManager.INTERVAL_WEEKLY -> start to start.plusDays(6)
                UsageStatsManager.INTERVAL_MONTHLY -> {
                    val m = start.withDayOfMonth(1)
                    m to m.plusMonths(1).minusDays(1)
                }
                else -> {
                    val y = start.withDayOfYear(1)
                    y to y.plusYears(1).minusDays(1)
                }
            }
            val cap = (last.toEpochDay() - first.toEpochDay() + 1) * DAY_MS
            val bucket = Bucket(pkg, first.toEpochDay(), last.toEpochDay(), minOf(ms, cap))
            merged.merge(pkg to bucket.start, bucket) { a, b -> if (a.millis >= b.millis) a else b }
        }
        return merged.values.toList()
    }

    private fun maxSpanMs(type: Int): Long = when (type) {
        UsageStatsManager.INTERVAL_DAILY -> 2 * DAY_MS
        UsageStatsManager.INTERVAL_WEEKLY -> 9 * DAY_MS
        UsageStatsManager.INTERVAL_MONTHLY -> 35 * DAY_MS
        else -> 400 * DAY_MS
    }

    companion object {
        /** Глубже трёх лет Android данных не держит. */
        const val WINDOW_DAYS = 3L * 366

        private const val DAY_MS = 86_400_000L
        private const val HALF_DAY_MS = DAY_MS / 2
        private const val MIN_REST_MS = 60_000.0
        private const val MIN_ROW_MS = 1_000L
        private const val NOISE_SHARE = 0.1

        private val LEVELS = intArrayOf(
            UsageStatsManager.INTERVAL_DAILY,
            UsageStatsManager.INTERVAL_WEEKLY,
            UsageStatsManager.INTERVAL_MONTHLY,
            UsageStatsManager.INTERVAL_YEARLY,
        )
    }
}

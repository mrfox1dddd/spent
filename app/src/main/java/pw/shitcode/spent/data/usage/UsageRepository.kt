package pw.shitcode.spent.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pw.shitcode.spent.data.db.AppTotal
import pw.shitcode.spent.data.db.SpentDatabase
import pw.shitcode.spent.data.export.ExportRow
import pw.shitcode.spent.util.Access
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HomeSnapshot(
    val total: Long,
    val hours: LongArray,
    val yesterday: Long,
    val weekAvg: Long,
    val top: List<AppTotal>,
    val notifToday: Int,
)

class UsageRepository(context: Context) {
    private val ctx = context.applicationContext
    private val db = SpentDatabase.get(ctx)
    private val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)
    private val reader = UsageReader(ctx)

    /** Не показываем в статистике: сам Spent и домашние экраны (лаунчеры). Данные при этом хранятся. */
    private val excluded: List<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val pm = ctx.packageManager
        val homes = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        homes.map { it.activityInfo.packageName }.distinct()
    }

    /**
     * Пересчитывает период с начала дня прошлой синхронизации (но не глубже 6 дней назад,
     * дальше Android детальные события уже не отдаёт). Дни в этом окне перезаписываются целиком,
     * поэтому повторный вызов безопасен, а пропуск нескольких дней подряд не ломает данные.
     * Одновременные вызовы выстраиваются в очередь, а вызов сразу после предыдущего пропускается.
     */
    suspend fun sync(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!Access.hasUsage(ctx)) return@withContext false
        syncMutex.withLock {
            val nowMs = System.currentTimeMillis()
            val last = prefs.getLong(KEY_LAST_SYNC, 0L)
            if (!force && nowMs - last in 0 until MIN_INTERVAL_MS) return@withLock true

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val earliest = today.minusDays(MAX_EVENT_DAYS)
            val fromDate = if (last == 0L) {
                earliest
            } else {
                maxOf(Instant.ofEpochMilli(last).atZone(zone).toLocalDate(), earliest)
            }
            val fromMs = fromDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val result = reader.read(fromMs, nowMs, zone)
            val fromDay = fromDate.toEpochDay()
            db.withTransaction {
                val dao = db.usage()
                dao.deleteHourlyFrom(fromDay)
                dao.deleteDailyFrom(fromDay)
                dao.insertHourly(result.hourly)
                dao.insertDaily(result.daily)
                dao.pruneHourly(today.minusDays(HOURLY_KEEP_DAYS).toEpochDay())
            }

            // Глубокая история: при первом запуске и если синхронизацию пропустили дольше, чем Android хранит события
            val lastDate = if (last == 0L) null else Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
            val gap = lastDate != null && fromDate.isAfter(lastDate)
            if (!prefs.getBoolean(KEY_FILLED, false) || gap) {
                runCatching { fillFromSystem(today, fromDay, zone) }
                    .onSuccess { prefs.edit().putBoolean(KEY_FILLED, true).apply() }
            }

            prefs.edit().putLong(KEY_LAST_SYNC, nowMs).apply()
            true
        }
    }

    /** Дописывает приблизительные дни до fromDay из системных корзин, существующие дни не трогает. */
    private suspend fun fillFromSystem(today: LocalDate, fromDay: Long, zone: ZoneId) {
        val dao = db.usage()
        val existing = dao.rowsFrom(today.toEpochDay() - UsageArchive.WINDOW_DAYS)
        val rows = UsageArchive(ctx).build(existing, fromDay, today, zone)
        if (rows.isNotEmpty()) dao.insertDaily(rows)
    }

    /**
     * Вся сохранённая история по дням и приложениям, с теми же исключениями, что и в статистике
     * (сам Spent и лаунчеры), чтобы цифры в файле совпадали с экраном.
     */
    suspend fun exportRows(): List<ExportRow> = withContext(Dispatchers.IO) {
        val usage = db.usage().all().filter { it.pkg !in excluded }.associateBy { it.pkg to it.day }
        val notifs = db.notifications().all().filter { it.pkg !in excluded }.associateBy { it.pkg to it.day }
        val firstNotifDay = db.notifications().firstDay()
        val loader = AppInfoLoader(ctx)
        val labels = (usage.keys + notifs.keys).map { it.first }.distinct().associateWith { loader.label(it) }

        (usage.keys + notifs.keys).map { (pkg, day) ->
            val u = usage[pkg to day]
            val approx = u?.approx ?: false
            val notifCount = notifs[pkg to day]?.count
            ExportRow(
                date = LocalDate.ofEpochDay(day),
                pkg = pkg,
                label = labels.getValue(pkg),
                usageMs = u?.millis ?: 0L,
                launches = if (approx) null else (u?.launches ?: 0),
                // Нет строки: после начала подсчёта это честный ноль, до него данных просто нет
                notifications = notifCount ?: (if (firstNotifDay != null && day >= firstNotifDay) 0 else null),
                approx = approx,
            )
        }.sortedWith(compareBy({ it.date }, { it.pkg }))
    }

    suspend fun topToday(limit: Int): List<AppTotal> {
        val day = LocalDate.now().toEpochDay()
        return db.usage().topApps(day, day, limit, excluded)
    }

    suspend fun notificationsToday(): Int {
        val day = LocalDate.now().toEpochDay()
        return db.notifications().total(day, day)
    }

    suspend fun home(topLimit: Int): HomeSnapshot {
        val today = LocalDate.now().toEpochDay()
        val usage = db.usage()
        val totals = usage.dayTotals(today - 7, today, excluded).associate { it.day to it.millis }
        val hours = LongArray(24)
        usage.hoursOf(today, excluded).forEach { hours[it.hour.coerceIn(0, 23)] = it.millis }
        val weekAvg = (1..7).sumOf { totals[today - it] ?: 0L } / 7
        return HomeSnapshot(
            total = totals[today] ?: 0L,
            hours = hours,
            yesterday = totals[today - 1] ?: 0L,
            weekAvg = weekAvg,
            top = usage.topApps(today, today, topLimit, excluded),
            notifToday = db.notifications().total(today, today),
        )
    }

    suspend fun stats(range: StatsRange, appLimit: Int): StatsSnapshot {
        val usage = db.usage()
        val today = LocalDate.now()
        val todayDay = today.toEpochDay()
        val first = usage.firstDay()?.let { LocalDate.ofEpochDay(it) }
        val exactFrom = usage.firstExactDay()?.let { LocalDate.ofEpochDay(it) }
        fun effectiveDays(from: LocalDate): Int =
            (ChronoUnit.DAYS.between(maxOf(from, first ?: from), today) + 1).toInt().coerceAtLeast(1)

        return when (range) {
            StatsRange.Day -> {
                val hours = LongArray(24)
                usage.hoursOf(todayDay, excluded).forEach { hours[it.hour.coerceIn(0, 23)] = it.millis }
                StatsSnapshot(
                    range = range,
                    values = hours.toList(),
                    dates = emptyList(),
                    total = hours.sum(),
                    days = 1,
                    apps = usage.topApps(todayDay, todayDay, appLimit, excluded),
                )
            }
            StatsRange.Week, StatsRange.Month -> {
                val n = if (range == StatsRange.Week) 7 else 30
                val from = today.minusDays(n - 1L)
                val totals = usage.dayTotals(from.toEpochDay(), todayDay, excluded).associate { it.day to it.millis }
                val dates = (0 until n).map { from.plusDays(it.toLong()) }
                val values = dates.map { totals[it.toEpochDay()] ?: 0L }
                val approxDays = usage.approxDays(from.toEpochDay(), todayDay).toHashSet()
                StatsSnapshot(
                    range = range,
                    values = values,
                    dates = dates,
                    total = values.sum(),
                    days = effectiveDays(from),
                    apps = usage.topApps(from.toEpochDay(), todayDay, appLimit, excluded),
                    approx = dates.map { it.toEpochDay() in approxDays },
                    exactFrom = exactFrom,
                )
            }
            StatsRange.Year, StatsRange.All -> {
                val thisMonth = YearMonth.from(today)
                val startMonth = if (range == StatsRange.Year) thisMonth.minusMonths(11) else YearMonth.from(first ?: today)
                val from = startMonth.atDay(1)
                val byMonth = usage.dayTotals(from.toEpochDay(), todayDay, excluded)
                    .groupBy({ YearMonth.from(LocalDate.ofEpochDay(it.day)) }, { it.millis })
                    .mapValues { it.value.sum() }
                val months = generateSequence(startMonth) { it.plusMonths(1) }
                    .takeWhile { !it.isAfter(thisMonth) }
                    .toList()
                val values = months.map { byMonth[it] ?: 0L }
                val approxMonths = usage.approxDays(from.toEpochDay(), todayDay)
                    .map { YearMonth.from(LocalDate.ofEpochDay(it)) }
                    .toHashSet()
                StatsSnapshot(
                    range = range,
                    values = values,
                    dates = months.map { it.atDay(1) },
                    total = values.sum(),
                    days = effectiveDays(from),
                    apps = usage.topApps(from.toEpochDay(), todayDay, appLimit, excluded),
                    approx = months.map { it in approxMonths },
                    exactFrom = exactFrom,
                )
            }
        }
    }

    suspend fun appStats(pkg: String, range: StatsRange): AppSnapshot {
        val usage = db.usage()
        val today = LocalDate.now()
        val todayDay = today.toEpochDay()
        val first = usage.firstDay()?.let { LocalDate.ofEpochDay(it) }
        val exactFrom = usage.firstExactDay()?.let { LocalDate.ofEpochDay(it) }
        fun effectiveDays(from: LocalDate): Int =
            (ChronoUnit.DAYS.between(maxOf(from, first ?: from), today) + 1).toInt().coerceAtLeast(1)

        // from: первый день периода, dates и values: столбики
        val from: LocalDate
        val values: List<Long>
        var dates: List<LocalDate> = emptyList()
        var approx: List<Boolean> = emptyList()
        when (range) {
            StatsRange.Day -> {
                from = today
                val hours = LongArray(24)
                usage.hoursOfApp(pkg, todayDay).forEach { hours[it.hour.coerceIn(0, 23)] = it.millis }
                values = hours.toList()
            }
            StatsRange.Week, StatsRange.Month -> {
                val n = if (range == StatsRange.Week) 7 else 30
                from = today.minusDays(n - 1L)
                val totals = usage.dayTotalsOf(pkg, from.toEpochDay(), todayDay).associate { it.day to it.millis }
                dates = (0 until n).map { from.plusDays(it.toLong()) }
                values = dates.map { totals[it.toEpochDay()] ?: 0L }
                val approxDays = usage.approxDays(from.toEpochDay(), todayDay).toHashSet()
                approx = dates.map { it.toEpochDay() in approxDays }
            }
            StatsRange.Year, StatsRange.All -> {
                val thisMonth = YearMonth.from(today)
                val startMonth = if (range == StatsRange.Year) thisMonth.minusMonths(11) else YearMonth.from(first ?: today)
                from = startMonth.atDay(1)
                val byMonth = usage.dayTotalsOf(pkg, from.toEpochDay(), todayDay)
                    .groupBy({ YearMonth.from(LocalDate.ofEpochDay(it.day)) }, { it.millis })
                    .mapValues { it.value.sum() }
                val months = generateSequence(startMonth) { it.plusMonths(1) }
                    .takeWhile { !it.isAfter(thisMonth) }
                    .toList()
                values = months.map { byMonth[it] ?: 0L }
                dates = months.map { it.atDay(1) }
                val approxMonths = usage.approxDays(from.toEpochDay(), todayDay)
                    .map { YearMonth.from(LocalDate.ofEpochDay(it)) }
                    .toHashSet()
                approx = months.map { it in approxMonths }
            }
        }
        val fromDay = from.toEpochDay()
        return AppSnapshot(
            range = range,
            values = values,
            dates = dates,
            approx = approx,
            total = values.sum(),
            days = if (range == StatsRange.Day) 1 else effectiveDays(from),
            launches = usage.launchesOf(pkg, fromDay, todayDay),
            notifications = db.notifications().totalOf(pkg, fromDay, todayDay),
            exactFrom = exactFrom,
        )
    }

    suspend fun notifStats(range: StatsRange, appLimit: Int): NotifSnapshot {
        val dao = db.notifications()
        val today = LocalDate.now()
        val todayDay = today.toEpochDay()
        val since = dao.firstDay()?.let { LocalDate.ofEpochDay(it) }
        fun effectiveDays(from: LocalDate): Int =
            (ChronoUnit.DAYS.between(maxOf(from, since ?: from), today) + 1).toInt().coerceAtLeast(1)

        return when (range) {
            StatsRange.Day -> {
                val hours = LongArray(24)
                dao.hoursOf(todayDay).forEach { hours[it.hour.coerceIn(0, 23)] = it.count.toLong() }
                NotifSnapshot(
                    range = range,
                    values = hours.toList(),
                    dates = emptyList(),
                    total = dao.total(todayDay, todayDay).toLong(),
                    days = 1,
                    apps = dao.top(todayDay, todayDay, appLimit),
                    since = since,
                )
            }
            StatsRange.Week, StatsRange.Month -> {
                val n = if (range == StatsRange.Week) 7 else 30
                val from = today.minusDays(n - 1L)
                val counts = dao.dayCounts(from.toEpochDay(), todayDay).associate { it.day to it.count.toLong() }
                val dates = (0 until n).map { from.plusDays(it.toLong()) }
                val values = dates.map { counts[it.toEpochDay()] ?: 0L }
                NotifSnapshot(
                    range = range,
                    values = values,
                    dates = dates,
                    total = values.sum(),
                    days = effectiveDays(from),
                    apps = dao.top(from.toEpochDay(), todayDay, appLimit),
                    since = since,
                )
            }
            StatsRange.Year, StatsRange.All -> {
                val thisMonth = YearMonth.from(today)
                val startMonth = if (range == StatsRange.Year) thisMonth.minusMonths(11) else YearMonth.from(since ?: today)
                val from = startMonth.atDay(1)
                val byMonth = dao.dayCounts(from.toEpochDay(), todayDay)
                    .groupBy({ YearMonth.from(LocalDate.ofEpochDay(it.day)) }, { it.count.toLong() })
                    .mapValues { it.value.sum() }
                val months = generateSequence(startMonth) { it.plusMonths(1) }
                    .takeWhile { !it.isAfter(thisMonth) }
                    .toList()
                val values = months.map { byMonth[it] ?: 0L }
                NotifSnapshot(
                    range = range,
                    values = values,
                    dates = months.map { it.atDay(1) },
                    total = values.sum(),
                    days = effectiveDays(from),
                    apps = dao.top(from.toEpochDay(), todayDay, appLimit),
                    since = since,
                )
            }
        }
    }

    private companion object {
        val syncMutex = Mutex()
        const val KEY_LAST_SYNC = "last_sync"
        const val KEY_FILLED = "archive_filled"
        const val MAX_EVENT_DAYS = 6L
        const val HOURLY_KEEP_DAYS = 35L
        const val MIN_INTERVAL_MS = 15_000L
    }
}

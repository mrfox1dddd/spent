package pw.shitcode.spent.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import pw.shitcode.spent.data.db.DailyUsage
import pw.shitcode.spent.data.db.HourlyUsage
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Превращает события UsageEvents в интервалы переднего плана и раскладывает их по часам.
 * Интервал = от первого ACTIVITY_RESUMED до момента, когда у приложения не осталось активных экранов,
 * либо до выключения экрана / выключения устройства / конца периода.
 */
class UsageReader(private val ctx: Context) {

    class Result(val hourly: List<HourlyUsage>, val daily: List<DailyUsage>)

    private class Open {
        val classes = HashSet<String>()
        var start = 0L
        var lastEnd = 0L
    }

    fun read(fromMs: Long, toMs: Long, zone: ZoneId = ZoneId.systemDefault()): Result {
        val usm = ctx.getSystemService(UsageStatsManager::class.java)
            ?: return Result(emptyList(), emptyList())
        val events = usm.queryEvents(fromMs, toMs)
        val event = UsageEvents.Event()

        val state = HashMap<String, Open>()
        val millis = HashMap<Triple<String, Long, Int>, Long>()
        val launches = HashMap<Pair<String, Long>, Int>()

        fun dayOf(ts: Long) = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate().toEpochDay()

        fun addInterval(pkg: String, start: Long, end: Long) {
            var cursor = start
            while (cursor < end) {
                val zdt = Instant.ofEpochMilli(cursor).atZone(zone)
                val hourEnd = zdt.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
                val segEnd = minOf(end, hourEnd)
                val key = Triple(pkg, zdt.toLocalDate().toEpochDay(), zdt.hour)
                millis.merge(key, segEnd - cursor, Long::plus)
                cursor = segEnd
            }
        }

        fun close(pkg: String, st: Open, end: Long) {
            if (st.start == 0L) return
            if (end > st.start) addInterval(pkg, st.start, end)
            st.lastEnd = end
            st.start = 0L
            st.classes.clear()
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val ts = event.timeStamp
            when (event.eventType) {
                FOREGROUND -> {
                    val pkg = event.packageName ?: continue
                    val st = state.getOrPut(pkg) { Open() }
                    if (st.start == 0L) {
                        st.start = ts
                        // переход между экранами одного приложения за долю секунды запуском не считаем
                        if (ts - st.lastEnd > MERGE_GAP_MS) launches.merge(pkg to dayOf(ts), 1, Int::plus)
                    }
                    st.classes.add(event.className ?: "")
                }
                BACKGROUND, STOPPED -> {
                    val pkg = event.packageName ?: continue
                    val st = state[pkg] ?: continue
                    st.classes.remove(event.className ?: "")
                    if (st.classes.isEmpty()) close(pkg, st, ts)
                }
                SCREEN_OFF, SHUTDOWN -> state.forEach { (pkg, st) -> close(pkg, st, ts) }
            }
        }
        state.forEach { (pkg, st) -> close(pkg, st, toMs) }

        val hourly = millis.map { (k, v) -> HourlyUsage(k.first, k.second, k.third, v) }
        val dailyMs = HashMap<Pair<String, Long>, Long>()
        hourly.forEach { dailyMs.merge(it.pkg to it.day, it.millis, Long::plus) }
        val daily = (dailyMs.keys + launches.keys).map { key ->
            DailyUsage(key.first, key.second, dailyMs[key] ?: 0L, launches[key] ?: 0)
        }
        return Result(hourly, daily)
    }

    /** Диагностика (временно): что система реально хранит в агрегатах по дням, месяцам и годам. */
    fun systemAggregates(zone: ZoneId = ZoneId.systemDefault()): String {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return ""
        val now = System.currentTimeMillis()
        val from = now - 3 * 365L * 24 * 3600 * 1000
        val sb = StringBuilder()
        fun block(name: String, type: Int, listAll: Boolean) {
            val byStart = java.util.TreeMap<java.time.LocalDate, Long>()
            (usm.queryUsageStats(type, from, now) ?: emptyList()).forEach { u ->
                val d = Instant.ofEpochMilli(u.firstTimeStamp).atZone(zone).toLocalDate()
                byStart.merge(d, u.totalTimeInForeground, Long::plus)
            }
            sb.append(name).append(": ").append(byStart.size).append('\n')
            if (listAll) {
                byStart.forEach { (d, ms) -> sb.append("  ").append(d).append("  ").append(ms / 60000).append(" min\n") }
            } else if (byStart.isNotEmpty()) {
                sb.append("  oldest ").append(byStart.firstKey()).append('\n')
            }
        }
        block("daily", UsageStatsManager.INTERVAL_DAILY, listAll = false)
        block("weekly", UsageStatsManager.INTERVAL_WEEKLY, listAll = false)
        block("monthly", UsageStatsManager.INTERVAL_MONTHLY, listAll = true)
        block("yearly", UsageStatsManager.INTERVAL_YEARLY, listAll = true)
        return sb.toString().trimEnd()
    }

    private companion object {
        const val MERGE_GAP_MS = 1_000L

        // Значения совпадают с UsageEvents.Event.*; литералами, чтобы не ловить deprecated на старых именах.
        const val FOREGROUND = 1        // ACTIVITY_RESUMED / MOVE_TO_FOREGROUND
        const val BACKGROUND = 2        // ACTIVITY_PAUSED / MOVE_TO_BACKGROUND
        const val SCREEN_OFF = 16       // SCREEN_NON_INTERACTIVE
        const val STOPPED = 23          // ACTIVITY_STOPPED
        const val SHUTDOWN = 26         // DEVICE_SHUTDOWN
    }
}

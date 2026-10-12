package pw.shitcode.spent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsageDao {
    @Query("SELECT MIN(day) FROM daily_usage")
    suspend fun firstDay(): Long?

    /** Вся история для экспорта. */
    @Query("SELECT * FROM daily_usage")
    suspend fun all(): List<DailyUsage>

    /** Первый день с точными данными (по событиям), всё что раньше восстановлено приблизительно. */
    @Query("SELECT MIN(day) FROM daily_usage WHERE approx = 0")
    suspend fun firstExactDay(): Long?

    @Query("SELECT * FROM daily_usage WHERE day >= :fromDay")
    suspend fun rowsFrom(fromDay: Long): List<DailyUsage>

    @Query("SELECT DISTINCT day FROM daily_usage WHERE approx = 1 AND day BETWEEN :from AND :to")
    suspend fun approxDays(from: Long, to: Long): List<Long>

    @Query("DELETE FROM hourly_usage WHERE day >= :fromDay")
    suspend fun deleteHourlyFrom(fromDay: Long)

    @Query("DELETE FROM daily_usage WHERE day >= :fromDay")
    suspend fun deleteDailyFrom(fromDay: Long)

    @Query("DELETE FROM hourly_usage WHERE day < :beforeDay")
    suspend fun pruneHourly(beforeDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourly(items: List<HourlyUsage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDaily(items: List<DailyUsage>)

    @Query(
        "SELECT pkg, SUM(millis) AS millis, SUM(launches) AS launches FROM daily_usage " +
            "WHERE day BETWEEN :from AND :to AND pkg NOT IN (:excluded) GROUP BY pkg ORDER BY millis DESC LIMIT :limit",
    )
    suspend fun topApps(from: Long, to: Long, limit: Int, excluded: List<String>): List<AppTotal>

    @Query("SELECT hour, SUM(millis) AS millis FROM hourly_usage WHERE day = :day AND pkg NOT IN (:excluded) GROUP BY hour")
    suspend fun hoursOf(day: Long, excluded: List<String>): List<HourTotal>

    @Query("SELECT day, SUM(millis) AS millis FROM daily_usage WHERE day BETWEEN :from AND :to AND pkg NOT IN (:excluded) GROUP BY day ORDER BY day")
    suspend fun dayTotals(from: Long, to: Long, excluded: List<String>): List<DayTotal>

    @Query("SELECT day, SUM(millis) AS millis FROM daily_usage WHERE pkg = :pkg AND day BETWEEN :from AND :to GROUP BY day ORDER BY day")
    suspend fun dayTotalsOf(pkg: String, from: Long, to: Long): List<DayTotal>

    @Query("SELECT hour, SUM(millis) AS millis FROM hourly_usage WHERE pkg = :pkg AND day = :day GROUP BY hour")
    suspend fun hoursOfApp(pkg: String, day: Long): List<HourTotal>

    @Query("SELECT COALESCE(SUM(launches), 0) FROM daily_usage WHERE pkg = :pkg AND day BETWEEN :from AND :to")
    suspend fun launchesOf(pkg: String, from: Long, to: Long): Int
}

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensure(item: NotificationDaily)

    @Query("UPDATE notification_daily SET count = count + 1 WHERE pkg = :pkg AND day = :day")
    suspend fun increment(pkg: String, day: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureHourly(item: NotificationHourly)

    @Query("UPDATE notification_hourly SET count = count + 1 WHERE pkg = :pkg AND day = :day AND hour = :hour")
    suspend fun incrementHourly(pkg: String, day: Long, hour: Int)

    @Query("SELECT MIN(day) FROM notification_daily")
    suspend fun firstDay(): Long?

    /** Вся история для экспорта. */
    @Query("SELECT * FROM notification_daily")
    suspend fun all(): List<NotificationDaily>

    @Query("SELECT day, SUM(count) AS count FROM notification_daily WHERE day BETWEEN :from AND :to GROUP BY day ORDER BY day")
    suspend fun dayCounts(from: Long, to: Long): List<DayCount>

    @Query("SELECT hour, SUM(count) AS count FROM notification_hourly WHERE day = :day GROUP BY hour")
    suspend fun hoursOf(day: Long): List<HourCount>

    @Query(
        "SELECT pkg, SUM(count) AS count FROM notification_daily " +
            "WHERE day BETWEEN :from AND :to GROUP BY pkg ORDER BY count DESC LIMIT :limit",
    )
    suspend fun top(from: Long, to: Long, limit: Int): List<NotifTotal>

    @Query("SELECT COALESCE(SUM(count), 0) FROM notification_daily WHERE day BETWEEN :from AND :to")
    suspend fun total(from: Long, to: Long): Int

    @Query("SELECT COALESCE(SUM(count), 0) FROM notification_daily WHERE pkg = :pkg AND day BETWEEN :from AND :to")
    suspend fun totalOf(pkg: String, from: Long, to: Long): Int
}

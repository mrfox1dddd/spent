package pw.shitcode.spent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [HourlyUsage::class, DailyUsage::class, NotificationDaily::class, NotificationHourly::class],
    version = 3,
    exportSchema = false,
)
abstract class SpentDatabase : RoomDatabase() {
    abstract fun usage(): UsageDao
    abstract fun notifications(): NotificationDao

    suspend fun countNotification(pkg: String, day: Long, hour: Int) = withTransaction {
        notifications().ensure(NotificationDaily(pkg, day, 0))
        notifications().increment(pkg, day)
        notifications().ensureHourly(NotificationHourly(pkg, day, hour, 0))
        notifications().incrementHourly(pkg, day, hour)
    }

    companion object {
        @Volatile private var instance: SpentDatabase? = null

        /** Счётчики уведомлений нельзя восстановить, поэтому миграция без пересоздания базы. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_usage ADD COLUMN approx INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notification_hourly` (`pkg` TEXT NOT NULL, `day` INTEGER NOT NULL, " +
                        "`hour` INTEGER NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`pkg`, `day`, `hour`))",
                )
            }
        }

        fun get(context: Context): SpentDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, SpentDatabase::class.java, "spent.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}

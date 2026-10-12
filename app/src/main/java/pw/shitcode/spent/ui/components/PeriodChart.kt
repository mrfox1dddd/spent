package pw.shitcode.spent.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import pw.shitcode.spent.data.usage.StatsRange
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * График периода с подписью выбранного столбика и осью: часы для Day, дни для Week/Month, месяцы для Year/All.
 * Сам хранит выбор (по умолчанию текущий час или последний столбик). format превращает значение в текст.
 */
@Composable
fun PeriodChart(
    range: StatsRange,
    values: List<Long>,
    dates: List<LocalDate>,
    approx: List<Boolean>,
    format: @Composable (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMMM", locale) }
    val monthFmt = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    val weekdayShort = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    val monthShort = remember(locale) { DateTimeFormatter.ofPattern("LLL", locale) }

    val n = values.size
    if (n == 0) return
    var picked by remember(range) { mutableIntStateOf(-1) }
    val sel = when {
        picked in 0 until n -> picked
        range == StatsRange.Day -> LocalTime.now().hour.coerceIn(0, n - 1)
        else -> n - 1
    }

    val tip = when (range) {
        StatsRange.Day -> "$sel:00"
        StatsRange.Week, StatsRange.Month -> dates[sel].format(dayFmt)
        StatsRange.Year, StatsRange.All -> dates[sel].format(monthFmt)
    }
    val isApprox = approx.getOrElse(sel) { false }
    val readout = (if (isApprox) "≈ " else "") + tip + ": " + format(values[sel])

    val axis: List<String> = when (range) {
        StatsRange.Day -> listOf("0:00", "6:00", "12:00", "18:00", "23:00")
        StatsRange.Week -> dates.map { it.format(weekdayShort) }
        else -> listOf(0f, 1f / 3, 2f / 3, 1f)
            .map { ((n - 1) * it).roundToInt() }
            .distinct()
            .map { i ->
                if (range == StatsRange.Month) dates[i].dayOfMonth.toString() else dates[i].format(monthShort)
            }
    }

    BarChart(
        values = values,
        axis = axis,
        selected = sel,
        onSelect = { picked = it },
        readout = readout,
        animateKey = range,
        modifier = modifier,
        approx = approx,
    )
}

package pw.shitcode.spent.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.R
import pw.shitcode.spent.data.usage.StatsRange
import pw.shitcode.spent.ui.components.AppRow
import pw.shitcode.spent.ui.components.BarChart
import pw.shitcode.spent.ui.components.EmptyAccess
import pw.shitcode.spent.ui.components.SegmentedControl
import pw.shitcode.spent.ui.components.SkeletonBlock
import pw.shitcode.spent.ui.components.SpentText
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.util.Access
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun NotificationsScreen(vm: NotifViewModel, access: Boolean, onOpenApp: (String) -> Unit) {
    val c = Spent.colors
    val ctx = LocalContext.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 22.dp, bottom = 120.dp + navBottom),
    ) {
        SpentText(
            stringResource(R.string.tab_notifications),
            style = Spent.type.h1,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (!access) {
            EmptyAccess(
                title = stringResource(R.string.empty_notif_title),
                body = stringResource(R.string.empty_notif_body),
                button = stringResource(R.string.access_grant),
                onGrant = { Access.openNotificationSettings(ctx) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            return@Column
        }

        SegmentedControl(
            options = listOf(
                StatsRange.Day to stringResource(R.string.range_day),
                StatsRange.Week to stringResource(R.string.range_week),
                StatsRange.Month to stringResource(R.string.range_month),
                StatsRange.Year to stringResource(R.string.range_year),
                StatsRange.All to stringResource(R.string.range_all),
            ),
            selected = vm.range,
            onSelect = vm::selectRange,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 22.dp),
        )

        val data = vm.data
        if (data == null) NotifSkeleton() else NotifContent(data, onOpenApp)
    }
}

@Composable
private fun NotifContent(data: NotifData, onOpenApp: (String) -> Unit) {
    val c = Spent.colors
    val s = data.snapshot
    val locale = LocalConfiguration.current.locales[0]
    val dayFmt = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMMM", locale) }
    val monthFmt = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    val weekdayShort = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    val monthShort = remember(locale) { DateTimeFormatter.ofPattern("LLL", locale) }
    val dateFmt = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    val n = s.values.size
    var picked by remember(s.range) { mutableIntStateOf(-1) }
    val sel = when {
        picked in 0 until n -> picked
        s.range == StatsRange.Day -> LocalTime.now().hour.coerceIn(0, n - 1)
        else -> n - 1
    }

    val tip = when (s.range) {
        StatsRange.Day -> "$sel:00"
        StatsRange.Week, StatsRange.Month -> s.dates[sel].format(dayFmt)
        StatsRange.Year, StatsRange.All -> s.dates[sel].format(monthFmt)
    }
    val picked1 = s.values[sel].toInt()
    val readout = tip + ": " + pluralStringResource(R.plurals.notifications, picked1, picked1)

    val axis: List<String> = when (s.range) {
        StatsRange.Day -> listOf("0:00", "6:00", "12:00", "18:00", "23:00")
        StatsRange.Week -> s.dates.map { it.format(weekdayShort) }
        else -> listOf(0f, 1f / 3, 2f / 3, 1f)
            .map { ((n - 1) * it).roundToInt() }
            .distinct()
            .map { i ->
                if (s.range == StatsRange.Month) s.dates[i].dayOfMonth.toString() else s.dates[i].format(monthShort)
            }
    }

    val base = stringResource(
        when (s.range) {
            StatsRange.Day -> R.string.cap_day
            StatsRange.Week -> R.string.cap_week
            StatsRange.Month -> R.string.cap_month
            StatsRange.Year -> R.string.cap_year
            StatsRange.All -> R.string.cap_all
        },
    )
    val total = s.total.toInt()
    val lead = pluralStringResource(R.plurals.notifications_word, total) + " " + base
    val caption = if (s.range == StatsRange.Day) {
        lead
    } else {
        stringResource(R.string.notif_caption_avg, lead, (s.total / s.days).toInt())
    }

    // длинные числа не должны вылезать за экран
    val digits = s.total.toString().length
    val size = when {
        digits <= 3 -> 88.sp
        digits == 4 -> 72.sp
        else -> 56.sp
    }
    SpentText(
        text = s.total.toString(),
        style = Spent.type.big.copy(fontSize = size, lineHeight = size),
        color = c.ac,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp),
    )
    SpentText(
        text = caption,
        style = Spent.type.small.copy(fontSize = 14.sp),
        color = c.tv,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp),
    )

    BarChart(
        values = s.values,
        axis = axis,
        selected = sel,
        onSelect = { picked = it },
        readout = readout,
        animateKey = s.range,
        modifier = Modifier.padding(top = 24.dp),
    )

    // истории до выдачи доступа нет, честно говорим об этом
    val since = s.since
    SpentText(
        text = if (since != null) {
            stringResource(R.string.notif_since, since.format(dateFmt))
        } else {
            stringResource(R.string.notifications_note)
        },
        style = Spent.type.small.copy(fontSize = 12.sp),
        color = c.tv,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp),
    )

    Column(Modifier.padding(horizontal = 20.dp)) {
        SpentText(
            text = stringResource(R.string.notif_loudest),
            style = Spent.type.h2,
            color = c.tv,
            modifier = Modifier.padding(top = 40.dp, bottom = 2.dp),
        )
        if (data.apps.isEmpty()) {
            SpentText(stringResource(R.string.empty_period), color = c.tv, modifier = Modifier.padding(top = 12.dp))
        }
        val topCount = data.apps.firstOrNull()?.millis ?: 1L
        data.apps.forEachIndexed { index, app ->
            key(s.range, app.pkg) {
                AppRow(
                    rank = index + 1,
                    label = app.label,
                    icon = app.icon,
                    share = if (s.total > 0) (app.millis * 100 / s.total).toInt() else 0,
                    value = app.millis.toString(),
                    fraction = app.millis.toFloat() / topCount,
                    top = index == 0,
                    onClick = { onOpenApp(app.pkg) },
                )
            }
        }
    }
}

@Composable
private fun NotifSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp)) {
        SkeletonBlock(RoundedCornerShape(16.dp), Modifier.fillMaxWidth(0.4f).height(80.dp))
        SkeletonBlock(RoundedCornerShape(10.dp), Modifier.padding(top = 12.dp).fillMaxWidth(0.5f).height(18.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 24.dp).fillMaxWidth().height(180.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp))
    }
}

package pw.shitcode.spent.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.R
import pw.shitcode.spent.data.usage.StatsRange
import pw.shitcode.spent.ui.components.IconId
import pw.shitcode.spent.ui.components.PeriodChart
import pw.shitcode.spent.ui.components.SegmentedControl
import pw.shitcode.spent.ui.components.SkeletonBlock
import pw.shitcode.spent.ui.components.SpentIcon
import pw.shitcode.spent.ui.components.SpentText
import pw.shitcode.spent.ui.durationBig
import pw.shitcode.spent.ui.formatDuration
import pw.shitcode.spent.ui.theme.Spent
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AppDetailScreen(vm: AppDetailViewModel, notifAccess: Boolean, onBack: () -> Unit) {
    val c = Spent.colors
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val data = vm.data

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(top = 22.dp, bottom = 120.dp + navBottom),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val back = stringResource(R.string.back)
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(shape)
                    .border(1.dp, c.line, shape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = back },
                contentAlignment = Alignment.Center,
            ) {
                SpentIcon(IconId.Back)
            }
            if (data?.icon != null) {
                Image(
                    bitmap = data.icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
            SpentText(
                text = data?.label.orEmpty(),
                style = Spent.type.h1.copy(fontSize = 22.sp, lineHeight = 28.sp),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
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

        if (data == null) DetailSkeleton() else DetailContent(data, notifAccess)
    }
}

@Composable
private fun DetailContent(data: AppDetailData, notifAccess: Boolean) {
    val c = Spent.colors
    val s = data.snapshot
    val locale = LocalConfiguration.current.locales[0]
    val dateFmt = androidx.compose.runtime.remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
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

    SpentText(
        text = durationBig(s.total),
        style = Spent.type.big,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 20.dp),
    )
    SpentText(
        text = stringResource(R.string.detail_caption, base),
        style = Spent.type.small.copy(fontSize = 14.sp),
        color = c.tv,
        modifier = Modifier.padding(horizontal = 20.dp).padding(top = 8.dp),
    )

    PeriodChart(
        range = s.range,
        values = s.values,
        dates = s.dates,
        approx = s.approx,
        format = { formatDuration(it) },
        modifier = Modifier.padding(top = 24.dp),
    )

    val hasApprox = s.approx.any { it }
    val exactFrom = s.exactFrom
    if (hasApprox && exactFrom != null) {
        SpentText(
            text = stringResource(R.string.stats_approx_note, exactFrom.format(dateFmt)),
            style = Spent.type.small.copy(fontSize = 12.sp),
            color = c.tv,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 14.dp),
        )
    }

    // Запусков / Уведомлений
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp)
            .fillMaxWidth()
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(c.line, Offset.Zero, Size(size.width, w))
                drawRect(c.line, Offset(0f, size.height - w), Size(size.width, w))
            }
            .height(IntrinsicSize.Min),
    ) {
        Fact(
            caption = stringResource(R.string.fact_launches),
            value = if (hasApprox && s.launches > 0) "≥ ${s.launches}" else s.launches.toString(),
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
        )
        Box(Modifier.fillMaxHeight().width(1.dp).background(c.line))
        Fact(
            caption = stringResource(R.string.fact_notifications),
            value = if (notifAccess) s.notifications.toString() else stringResource(R.string.no_access),
            modifier = Modifier.weight(1f).padding(start = 18.dp, top = 14.dp, bottom = 14.dp),
        )
    }
    if (hasApprox) {
        SpentText(
            text = stringResource(R.string.detail_launches_note),
            style = Spent.type.small.copy(fontSize = 12.sp),
            color = c.tv,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 12.dp),
        )
    }
}

@Composable
private fun Fact(caption: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        SpentText(caption, style = Spent.type.small, color = Spent.colors.tv)
        SpentText(value, style = Spent.type.bodyStrong.copy(fontSize = 18.sp, lineHeight = 26.sp), maxLines = 1)
    }
}

@Composable
private fun DetailSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 20.dp)) {
        SkeletonBlock(RoundedCornerShape(16.dp), Modifier.fillMaxWidth(0.6f).height(56.dp))
        SkeletonBlock(RoundedCornerShape(10.dp), Modifier.padding(top = 12.dp).fillMaxWidth(0.4f).height(18.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 24.dp).fillMaxWidth().height(180.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 24.dp).fillMaxWidth().height(56.dp))
    }
}

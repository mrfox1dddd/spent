package pw.shitcode.spent.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.R
import pw.shitcode.spent.ui.components.AppRow
import pw.shitcode.spent.ui.components.Dial
import pw.shitcode.spent.ui.components.EmptyAccess
import pw.shitcode.spent.ui.components.IconId
import pw.shitcode.spent.ui.components.SkeletonBlock
import pw.shitcode.spent.ui.components.SpentIcon
import pw.shitcode.spent.ui.components.SpentText
import pw.shitcode.spent.ui.formatDuration
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.ui.theme.Unbounded
import pw.shitcode.spent.util.Access
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(vm: HomeViewModel, onOpenNotifications: () -> Unit, onOpenApp: (String) -> Unit) {
    val c = Spent.colors
    val ctx = LocalContext.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(locale) { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM", locale)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 120.dp + navBottom),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            SpentText(
                stringResource(R.string.home_title),
                style = Spent.type.small.copy(fontFamily = Unbounded, fontWeight = FontWeight.Medium),
            )
            SpentText(date, style = Spent.type.small, color = c.tv)
        }

        val data = vm.home
        when {
            !vm.usageAccess -> EmptyAccess(
                title = stringResource(R.string.empty_usage_title),
                body = stringResource(R.string.empty_usage_body),
                button = stringResource(R.string.access_grant),
                onGrant = { Access.openUsageSettings(ctx) },
            )
            data == null -> HomeSkeleton()
            else -> HomeContent(data, vm.notifAccess, onOpenNotifications, onOpenApp)
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    notifAccess: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenApp: (String) -> Unit,
) {
    val c = Spent.colors
    val currentHour = remember { LocalTime.now().hour }
    var selected by rememberSaveable { mutableIntStateOf(currentHour) }
    val readout = "$selected:00: " + formatDuration(data.hours[selected])

    Dial(
        hours = data.hours,
        currentHour = currentHour,
        selected = selected,
        onSelect = { selected = it },
        total = data.total,
        modifier = Modifier.padding(top = 26.dp),
        description = readout,
    )
    SpentText(
        text = readout,
        style = Spent.type.label,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .heightIn(min = 21.dp),
    )

    // Вчера / в среднем за неделю
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(c.line, Offset.Zero, Size(size.width, w))
                drawRect(c.line, Offset(0f, size.height - w), Size(size.width, w))
            }
            .height(IntrinsicSize.Min),
    ) {
        Fact(stringResource(R.string.yesterday), formatDuration(data.yesterday), Modifier.weight(1f).padding(vertical = 14.dp))
        Box(Modifier.fillMaxHeight().width(1.dp).background(c.line))
        Fact(
            stringResource(R.string.week_avg),
            formatDuration(data.weekAvg),
            Modifier.weight(1f).padding(start = 18.dp, top = 14.dp, bottom = 14.dp),
        )
    }

    SpentText(
        text = stringResource(R.string.most_time),
        style = Spent.type.h2,
        color = c.tv,
        modifier = Modifier.padding(top = 40.dp, bottom = 2.dp),
    )
    if (data.top.isEmpty()) {
        SpentText(stringResource(R.string.empty_today), color = c.tv, modifier = Modifier.padding(top = 12.dp))
    }
    val topMs = data.top.firstOrNull()?.millis ?: 1L
    data.top.forEachIndexed { index, app ->
        AppRow(
            rank = index + 1,
            label = app.label,
            icon = app.icon,
            share = if (data.total > 0) (app.millis * 100 / data.total).toInt() else 0,
            value = formatDuration(app.millis),
            fraction = app.millis.toFloat() / topMs,
            top = index == 0,
            onClick = { onOpenApp(app.pkg) },
        )
    }

    // Ссылка на уведомления
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp)
            .drawBehind {
                val w = 1.dp.toPx()
                drawRect(c.line, Offset.Zero, Size(size.width, w))
                drawRect(c.line, Offset(0f, size.height - w), Size(size.width, w))
            }
            .clickable(role = Role.Button, onClick = onOpenNotifications)
            .heightIn(min = 64.dp)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SpentIcon(IconId.Bell)
        SpentText(
            stringResource(R.string.notifications_today),
            style = Spent.type.small.copy(fontSize = 14.sp),
            color = c.tv,
            modifier = Modifier.weight(1f),
        )
        if (notifAccess) {
            SpentText(data.notifToday.toString(), style = Spent.type.stat, color = c.ac)
        } else {
            SpentText(stringResource(R.string.no_access), style = Spent.type.bodyStrong)
        }
    }
}

@Composable
private fun Fact(caption: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        SpentText(caption, style = Spent.type.small, color = Spent.colors.tv)
        SpentText(value, style = Spent.type.bodyStrong.copy(fontSize = 18.sp, lineHeight = 26.sp))
    }
}

@Composable
private fun HomeSkeleton() {
    Column(Modifier.fillMaxWidth().padding(top = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SkeletonBlock(CircleShape, Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 28.dp).fillMaxWidth().height(56.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp))
        SkeletonBlock(RoundedCornerShape(20.dp), Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp))
    }
}

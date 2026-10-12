package pw.shitcode.spent.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import pw.shitcode.spent.R
import pw.shitcode.spent.ui.components.SegmentedControl
import pw.shitcode.spent.ui.components.SpentButton
import pw.shitcode.spent.ui.components.SpentOutlineButton
import pw.shitcode.spent.ui.components.SpentText
import pw.shitcode.spent.ui.theme.Accent
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.ui.theme.ThemeMode
import pw.shitcode.spent.ui.theme.spentColors
import pw.shitcode.spent.util.Access
import java.time.LocalDate

@Composable
fun SettingsScreen(
    mode: ThemeMode,
    onMode: (ThemeMode) -> Unit,
    accent: Accent,
    onAccent: (Accent) -> Unit,
    usageAccess: Boolean,
    notifAccess: Boolean,
    exportStatus: ExportStatus,
    onExport: (Uri, ExportType) -> Unit,
) {
    val c = Spent.colors
    val ctx = LocalContext.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Активити сама обрабатывает смену локали (configChanges в манифесте) и не пересоздаётся,
    // поэтому выбранный язык держим в состоянии, а не читаем один раз
    var language by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().let { if (it.isEmpty) "" else it[0]?.language.orEmpty() })
    }
    val version = remember { versionName(ctx) }

    // Системный диалог сохранения (SAF): разрешения на память не нужны, пользователь сам выбирает место
    val stamp = remember { LocalDate.now().toString() }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) onExport(uri, ExportType.Csv)
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) onExport(uri, ExportType.Json)
    }
    val working = exportStatus is ExportStatus.Working

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 120.dp + navBottom),
    ) {
        SpentText(stringResource(R.string.tab_settings), style = Spent.type.h1)

        // Показываем только то, чего не хватает: выдать доступ можно лишь в системных настройках
        if (!usageAccess || !notifAccess) {
            Section(R.string.settings_access, bottom = 2.dp)
            if (!usageAccess) {
                AccessRow(
                    title = stringResource(R.string.access_usage),
                    description = stringResource(R.string.access_usage_desc),
                    onGrant = { Access.openUsageSettings(ctx) },
                )
            }
            if (!notifAccess) {
                AccessRow(
                    title = stringResource(R.string.access_notifications),
                    description = stringResource(R.string.access_notifications_desc),
                    onGrant = { Access.openNotificationSettings(ctx) },
                )
            }
        }

        Section(R.string.settings_appearance)
        SegmentedControl(
            options = listOf(
                ThemeMode.System to stringResource(R.string.theme_system),
                ThemeMode.Light to stringResource(R.string.theme_light),
                ThemeMode.Dark to stringResource(R.string.theme_dark),
            ),
            selected = mode,
            onSelect = onMode,
        )

        Section(R.string.settings_accent)
        AccentPicker(selected = accent, onSelect = onAccent)

        Section(R.string.settings_language)
        SegmentedControl(
            options = listOf(
                "" to stringResource(R.string.language_system),
                "en" to stringResource(R.string.language_en),
                "ru" to stringResource(R.string.language_ru),
            ),
            selected = language,
            onSelect = { tag ->
                language = tag
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(tag),
                )
            },
        )

        Section(R.string.settings_data)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpentOutlineButton(
                text = stringResource(R.string.export_csv),
                onClick = { csvLauncher.launch("spent-$stamp.csv") },
                modifier = Modifier.weight(1f),
                enabled = !working,
            )
            SpentOutlineButton(
                text = stringResource(R.string.export_json),
                onClick = { jsonLauncher.launch("spent-$stamp.json") },
                modifier = Modifier.weight(1f),
                enabled = !working,
            )
        }
        val statusText = when (exportStatus) {
            ExportStatus.Idle -> null
            ExportStatus.Working -> stringResource(R.string.export_working)
            is ExportStatus.Done ->
                if (exportStatus.rows == 0) stringResource(R.string.export_empty)
                else stringResource(R.string.export_done, exportStatus.rows)
            ExportStatus.Failed -> stringResource(R.string.export_failed)
        }
        if (statusText != null) {
            SpentText(
                text = statusText,
                style = Spent.type.label,
                color = if (exportStatus is ExportStatus.Done) c.ac else c.tx,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        SpentText(
            text = stringResource(R.string.export_note),
            style = Spent.type.small,
            color = c.tv,
            modifier = Modifier.padding(top = 12.dp),
        )

        Section(R.string.settings_about)
        SpentText(stringResource(R.string.about_body), color = c.tv)
        if (version.isNotEmpty()) {
            SpentText(
                text = stringResource(R.string.about_version, version),
                style = Spent.type.small,
                color = c.tv,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        SpentText(
            text = stringResource(R.string.about_license),
            style = Spent.type.small,
            color = c.tv,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Section(titleRes: Int, bottom: androidx.compose.ui.unit.Dp = 10.dp) {
    SpentText(
        text = stringResource(titleRes),
        style = Spent.type.h2,
        color = Spent.colors.tv,
        modifier = Modifier.padding(top = 40.dp, bottom = bottom),
    )
}

@Composable
private fun AccessRow(title: String, description: String, onGrant: () -> Unit) {
    val line = Spent.colors.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
            }
            .heightIn(min = 68.dp)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            SpentText(title)
            SpentText(description, style = Spent.type.small, color = Spent.colors.tv)
        }
        SpentButton(text = stringResource(R.string.access_grant), onClick = onGrant)
    }
}

@Composable
private fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit) {
    val c = Spent.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Accent.entries.forEach { accent ->
            val isSelected = accent == selected
            val shape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .border(1.dp, if (isSelected) c.tx else c.line, shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .padding(top = 14.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(spentColors(accent, dark = true).ac),
                )
                SpentText(
                    text = stringResource(accent.label),
                    style = Spent.type.small.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (isSelected) c.tx else c.tv,
                )
            }
        }
    }
}

private fun versionName(ctx: Context): String = runCatching {
    val pm = ctx.packageManager
    val info = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(ctx.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(ctx.packageName, 0)
    }
    info.versionName.orEmpty()
}.getOrDefault("")

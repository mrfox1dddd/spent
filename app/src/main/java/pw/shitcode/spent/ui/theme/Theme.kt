package pw.shitcode.spent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalSpentColors = staticCompositionLocalOf<SpentColors> { error("SpentTheme is missing") }
private val LocalSpentType = staticCompositionLocalOf { DefaultSpentType }

object Spent {
    val colors: SpentColors
        @Composable @ReadOnlyComposable get() = LocalSpentColors.current
    val type: SpentType
        @Composable @ReadOnlyComposable get() = LocalSpentType.current
}

@Composable
fun SpentTheme(
    mode: ThemeMode = ThemeMode.System,
    accent: Accent = Accent.Mint,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = remember(accent, dark) { spentColors(accent, dark) }
    CompositionLocalProvider(
        LocalSpentColors provides colors,
        LocalSpentType provides DefaultSpentType,
    ) {
        content()
    }
}

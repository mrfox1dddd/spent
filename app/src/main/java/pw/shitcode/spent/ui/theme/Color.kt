package pw.shitcode.spent.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import pw.shitcode.spent.R

enum class ThemeMode { System, Light, Dark }

enum class Accent(@StringRes val label: Int) {
    Mint(R.string.accent_mint),
    Rose(R.string.accent_rose),
    Amber(R.string.accent_amber),
}

@Immutable
class SpentColors(
    val dark: Boolean,
    val bg: Color,
    val s1: Color,
    val s2: Color,
    val tx: Color,
    val tv: Color,
    val line: Color,
    val bar: Color,
    val ac: Color,
    val onAc: Color,
)

private fun hex(rgb: String) = Color(("FF$rgb").toLong(16))

private fun accentPair(accent: Accent, dark: Boolean): Pair<String, String> = when (accent) {
    Accent.Mint -> if (dark) "3EE3A8" to "04140D" else "006E54" to "FFFFFF"
    Accent.Rose -> if (dark) "FF5C93" to "1A0610" else "C2185B" to "FFFFFF"
    Accent.Amber -> if (dark) "FFC233" to "1A1204" else "8A5A00" to "FFFFFF"
}

fun spentColors(accent: Accent, dark: Boolean): SpentColors {
    val (ac, onAc) = accentPair(accent, dark)
    return if (dark) {
        SpentColors(true, hex("0C0E10"), hex("14171A"), hex("1C2024"), hex("ECEFF1"), hex("9AA3AA"), hex("262B30"), hex("5F6A72"), hex(ac), hex(onAc))
    } else {
        SpentColors(false, hex("EEF0F2"), hex("F7F8F9"), hex("E2E5E8"), hex("14181B"), hex("566069"), hex("D3D8DC"), hex("7A858D"), hex(ac), hex(onAc))
    }
}

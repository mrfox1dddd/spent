package pw.shitcode.spent.ui.theme

import androidx.annotation.FontRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.R
import androidx.compose.ui.text.ExperimentalTextApi


@OptIn(ExperimentalTextApi::class)
private fun variable(@FontRes res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Onest = FontFamily(
    variable(R.font.onest_variable, 400),
    variable(R.font.onest_variable, 500),
    variable(R.font.onest_variable, 600),
)

val Unbounded = FontFamily(
    variable(R.font.unbounded_variable, 500),
    variable(R.font.unbounded_variable, 600),
)

@Immutable
class SpentType(
    val h1: TextStyle,
    val big: TextStyle,
    val stat: TextStyle,
    val h2: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val small: TextStyle,
)

val DefaultSpentType = SpentType(
    h1 = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Medium, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.03).em),
    big = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Medium, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-0.05).em),
    stat = TextStyle(fontFamily = Unbounded, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.04).em),
    h2 = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    body = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyStrong = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, fontFeatureSettings = "tnum"),
    label = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    small = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
)

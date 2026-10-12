package pw.shitcode.spent.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.R
import pw.shitcode.spent.ui.theme.Onest
import pw.shitcode.spent.ui.theme.Spent

@Composable
fun formatDuration(millis: Long): String {
    val totalMin = millis / 60_000
    val h = (totalMin / 60).toInt()
    val m = (totalMin % 60).toInt()
    return when {
        h > 0 -> stringResource(R.string.duration_h_min, h, m)
        totalMin > 0 -> stringResource(R.string.duration_min, m)
        millis <= 0 -> stringResource(R.string.duration_min, 0)
        else -> stringResource(R.string.duration_lt_min)
    }
}

/** Крупное время: цифры шрифтом стиля, единицы мелким Onest приглушённым цветом. */
@Composable
fun durationBig(millis: Long): AnnotatedString {
    val totalMin = millis / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    val unitH = stringResource(R.string.unit_h)
    val unitMin = stringResource(R.string.unit_min)
    val unit = SpanStyle(
        fontFamily = Onest,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        color = Spent.colors.tv,
    )
    return buildAnnotatedString {
        if (h > 0) {
            append(h.toString())
            withStyle(unit) { append(" $unitH  ") }
        }
        if (h < 100) {
            append(m.toString())
            withStyle(unit) { append(" $unitMin") }
        }
    }
}

package pw.shitcode.spent.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pw.shitcode.spent.ui.theme.Spent

enum class IconId { Home, Stats, Bell, Sliders, Back, Lock }

private val iconPaths = mapOf(
    IconId.Home to listOf("M4 11 12 4l8 7v9H4z", "M10 20v-5h4v5"),
    IconId.Stats to listOf("M6 20V12M12 20V5M18 20v-5"),
    IconId.Bell to listOf("M6 17v-6a6 6 0 0 1 12 0v6l1.5 2h-15z", "M10 21h4"),
    IconId.Sliders to listOf("M4 8h9M17 8h3M4 16h3M11 16h9"),
    IconId.Back to listOf("M15 5l-7 7 7 7"),
    IconId.Lock to listOf("M8 11V8a4 4 0 0 1 8 0v3"),
)

/** Иконки 24x24, линия 1.75, скруглённые концы. */
@Composable
fun SpentIcon(
    icon: IconId,
    modifier: Modifier = Modifier,
    tint: Color = Spent.colors.tx,
    size: Dp = 22.dp,
) {
    val paths = remember(icon) { iconPaths.getValue(icon).map { PathParser().parsePathString(it).toPath() } }
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        scale(this.size.width / 24f, pivot = Offset.Zero) {
            paths.forEach { drawPath(it, tint, style = stroke) }
            when (icon) {
                IconId.Sliders -> {
                    drawCircle(tint, radius = 2f, center = Offset(15f, 8f), style = stroke)
                    drawCircle(tint, radius = 2f, center = Offset(9f, 16f), style = stroke)
                }
                IconId.Lock -> drawRoundRect(
                    tint,
                    topLeft = Offset(5f, 11f),
                    size = Size(14f, 9f),
                    cornerRadius = CornerRadius(2f),
                    style = stroke,
                )
                else -> Unit
            }
        }
    }
}

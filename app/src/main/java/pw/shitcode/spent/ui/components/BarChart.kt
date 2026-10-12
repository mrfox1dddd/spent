package pw.shitcode.spent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.util.rememberReducedMotion
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val STAGGER_MS = 10
private const val GROW_MS = 500
private val growEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val SIDE_PAD = 20.dp

/**
 * Столбчатый график на всю ширину. Выбор: касание, горизонтальное перетаскивание, стрелки на клавиатуре.
 * animateKey: при смене столбики вырастают заново.
 */
@Composable
fun BarChart(
    values: List<Long>,
    axis: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    readout: String,
    animateKey: Any,
    modifier: Modifier = Modifier,
    /** Столбики, восстановленные из укрупнённых системных данных, рисуются бледнее. */
    approx: List<Boolean> = emptyList(),
) {
    val c = Spent.colors
    val n = values.size
    val reduced = rememberReducedMotion()
    val grow = remember(animateKey) { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(animateKey) {
        if (!reduced) grow.animateTo(1f, tween(GROW_MS + n * STAGGER_MS, easing = LinearEasing))
    }
    val onSel by rememberUpdatedState(onSelect)
    val sel by rememberUpdatedState(selected)
    val maxValue = max(values.maxOrNull() ?: 0L, 1L)

    Column(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = readout }
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { onSel(max(0, sel - 1)); true }
                    Key.DirectionRight -> { onSel(min(n - 1, sel + 1)); true }
                    else -> false
                }
            },
    ) {
        SpentText(
            text = readout,
            style = Spent.type.label,
            modifier = Modifier
                .padding(horizontal = SIDE_PAD)
                .padding(bottom = 12.dp)
                .heightIn(min = 21.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(n) {
                    detectTapGestures { onSel(indexAt(it.x, size.width.toFloat(), SIDE_PAD.toPx(), n)) }
                }
                .pointerInput(n) {
                    detectHorizontalDragGestures(
                        onDragStart = { onSel(indexAt(it.x, size.width.toFloat(), SIDE_PAD.toPx(), n)) },
                    ) { change, _ ->
                        change.consume()
                        onSel(indexAt(change.position.x, size.width.toFloat(), SIDE_PAD.toPx(), n))
                    }
                },
        ) {
            val line = 1.dp.toPx()
            drawRect(c.line, Offset(0f, 0f), Size(size.width, line))
            drawRect(c.line, Offset(0f, (size.height - line) / 2f), Size(size.width, line))
            drawRect(c.line, Offset(0f, size.height - line), Size(size.width, line))
            if (n == 0) return@Canvas

            val pad = SIDE_PAD.toPx()
            val gap = (if (n > 20) 2.dp else 4.dp).toPx()
            val barW = (size.width - 2 * pad - gap * (n - 1)) / n
            val total = (GROW_MS + n * STAGGER_MS).toFloat()
            val minH = 3.dp.toPx()
            for (i in 0 until n) {
                val local = ((grow.value * total - i * STAGGER_MS) / GROW_MS).coerceIn(0f, 1f)
                val e = growEasing.transform(local)
                val full = max(minH, values[i].toFloat() / maxValue * size.height)
                val h = full * (0.15f + 0.85f * e)
                val left = pad + i * (barW + gap)
                val top = size.height - h
                val big = min(5.dp.toPx(), barW / 2f)
                val small = min(2.dp.toPx(), barW / 2f)
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left, top = top, right = left + barW, bottom = size.height,
                            topLeftCornerRadius = CornerRadius(big),
                            topRightCornerRadius = CornerRadius(big),
                            bottomRightCornerRadius = CornerRadius(small),
                            bottomLeftCornerRadius = CornerRadius(small),
                        ),
                    )
                }
                val base = if (i == selected) c.ac else c.bar
                val soft = if (approx.getOrElse(i) { false }) 0.5f else 1f
                drawPath(path, base.copy(alpha = base.alpha * e * soft))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = SIDE_PAD)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            axis.forEach { SpentText(it, style = Spent.type.small.copy(fontSize = 12.sp), color = c.tv, maxLines = 1) }
        }
    }
}

private fun indexAt(x: Float, width: Float, pad: Float, n: Int): Int {
    val span = max(width - 2 * pad, 1f)
    return floor((x - pad) / span * n).toInt().coerceIn(0, max(n - 1, 0))
}

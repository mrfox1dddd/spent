package pw.shitcode.spent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.R
import pw.shitcode.spent.ui.durationBig
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.util.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private const val STAGGER_MS = 14
private const val POP_MS = 600
private val popEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/**
 * Суточный циферблат: 24 радиальных штриха, час 0 вверху. Длина штриха = время за час.
 * Выбранный час акцентом, будущие часы короткие тусклые. Выбор касанием и перетаскиванием.
 */
@Composable
fun Dial(
    hours: LongArray,
    currentHour: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    total: Long,
    modifier: Modifier = Modifier,
    description: String = "",
) {
    val c = Spent.colors
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) progress.animateTo(1f, tween(POP_MS + 23 * STAGGER_MS, easing = LinearEasing))
    }
    val onSel by rememberUpdatedState(onSelect)
    val maxHour = max(hours.maxOrNull() ?: 0L, 1L)

    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = description }
                .pointerInput(Unit) { detectTapGestures { pickHour(it, size, onSel) } }
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { pickHour(it, size, onSel) }) { change, _ ->
                        change.consume()
                        pickHour(change.position, size, onSel)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val s = size.width / 320f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(0f to c.ac.copy(alpha = 0.13f), 0.78f to Color.Transparent),
                        center = center,
                        radius = size.width / 2f,
                    ),
                    radius = size.width / 2f,
                    center = center,
                )
                val totalMs = (POP_MS + 23 * STAGGER_MS).toFloat()
                for (i in 0..23) {
                    val local = ((progress.value * totalMs - i * STAGGER_MS) / POP_MS).coerceIn(0f, 1f)
                    val e = popEasing.transform(local)
                    val angle = (i * 15 - 90) * PI / 180.0
                    val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val future = i > currentHour
                    val r0 = 92f
                    val r1 = r0 + if (future) 4f else 6f + hours[i].toFloat() / maxHour * 50f
                    val k = (0.7f + 0.3f * e) * s
                    val base = when {
                        i == selected -> c.ac
                        future -> c.line
                        else -> c.bar
                    }
                    drawLine(
                        color = base.copy(alpha = base.alpha * e),
                        start = center + dir * (r0 * k),
                        end = center + dir * (r1 * k),
                        strokeWidth = (if (future) 4f else 7f) * s,
                        cap = StrokeCap.Round,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SpentText(
                    text = stringResource(R.string.today_caption),
                    style = Spent.type.small,
                    color = c.tv,
                )
                SpentText(
                    text = durationBig(total),
                    style = Spent.type.big.copy(fontSize = 38.sp, lineHeight = 42.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun pickHour(p: Offset, size: IntSize, onSelect: (Int) -> Unit) {
    val dx = p.x - size.width / 2f
    val dy = p.y - size.height / 2f
    if (hypot(dx, dy) < size.width * 0.27f) return
    val deg = (atan2(dy, dx) * 180.0 / PI + 450.0) % 360.0
    onSelect((deg / 15.0).roundToInt() % 24)
}

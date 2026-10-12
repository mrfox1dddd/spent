package pw.shitcode.spent.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.util.rememberReducedMotion

/**
 * Только рисует переключатель 46x28. Нажатие обрабатывает строка вокруг (toggleable),
 * чтобы вся строка была целью касания, а в TalkBack читалась как одно целое.
 */
@Composable
fun SpentSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = Spent.colors
    val reduced = rememberReducedMotion()
    val shape = RoundedCornerShape(9.dp)
    // Внутри рамки в 1dp: 4dp отступа слева, ход бегунка 18dp
    val shift by animateDpAsState(
        targetValue = if (checked) 23.dp else 5.dp,
        animationSpec = if (reduced) snap() else tween(250, easing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)),
        label = "switch",
    )
    Box(
        modifier = modifier
            .size(width = 46.dp, height = 28.dp)
            .clip(shape)
            .background(if (checked) c.ac else c.s2)
            .border(1.dp, if (checked) c.ac else c.bar, shape),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = shift)
                .size(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) c.onAc else c.tv),
        )
    }
}

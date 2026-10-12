package pw.shitcode.spent.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.util.rememberReducedMotion

/** Заглушка на время загрузки. Мягкая пульсация между s1 и s2, при отключённых анимациях статична. */
@Composable
fun SkeletonBlock(shape: Shape, modifier: Modifier = Modifier) {
    val c = Spent.colors
    val reduced = rememberReducedMotion()
    val phase: State<Float> = if (reduced) {
        remember { mutableFloatStateOf(0f) }
    } else {
        rememberInfiniteTransition(label = "skeleton").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "skeletonPhase",
        )
    }
    Box(
        modifier
            .clip(shape)
            .drawBehind { drawRect(lerp(c.s1, c.s2, phase.value)) },
    )
}

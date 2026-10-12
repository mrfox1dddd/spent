package pw.shitcode.spent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pw.shitcode.spent.ui.theme.Spent
import pw.shitcode.spent.util.rememberReducedMotion

/** Строка списка: номер, иконка, название, доля, значение. Под строкой линия, залитая по величине. */
@Composable
fun AppRow(
    rank: Int,
    label: String,
    icon: ImageBitmap?,
    share: Int,
    value: String,
    fraction: Float,
    top: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = Spent.colors
    val reduced = rememberReducedMotion()
    val fill = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(rank * 40L)
            fill.animateTo(1f, tween(700, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        }
    }
    val fillColor = if (top) c.ac else c.bar
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val thin = 1.dp.toPx()
                val thick = 2.dp.toPx()
                drawRect(c.line, Offset(0f, size.height - thin), Size(size.width, thin))
                drawRect(
                    fillColor,
                    Offset(0f, size.height - thick),
                    Size(size.width * fraction.coerceIn(0f, 1f) * fill.value, thick),
                )
            }
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 68.dp)
            .padding(top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SpentText(
            text = rank.toString().padStart(2, '0'),
            style = Spent.type.small.copy(fontSize = 12.sp),
            color = c.tv,
            modifier = Modifier.width(20.dp),
        )
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
            )
        } else {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.s2))
        }
        Column(Modifier.weight(1f)) {
            SpentText(label, style = Spent.type.body.copy(fontWeight = FontWeight.Medium), maxLines = 1)
            SpentText("$share%", style = Spent.type.small, color = c.tv)
        }
        SpentText(value, style = Spent.type.bodyStrong, maxLines = 1)
    }
}


package pw.shitcode.spent.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pw.shitcode.spent.ui.theme.Spent

class DockItem(val icon: IconId, val label: String)

/**
 * Плавающий док. Настоящего размытия фона нет: в Compose нет backdrop-blur без сторонних библиотек,
 * поэтому полупрозрачная поверхность с тонкой рамкой.
 */
@Composable
fun SpentDock(
    items: List<DockItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spent.colors
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .clip(shape)
            .background(c.s1.copy(alpha = 0.92f))
            .border(1.dp, c.line, shape)
            .padding(6.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val active = index == selected
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (active) c.ac else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
                    // размер анимируем внутри clip/background: таблетка растёт целиком, края круглые на каждом кадре
                    .animateContentSize()
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                SpentIcon(item.icon, tint = if (active) c.onAc else c.tv)
                if (active) {
                    SpentText(
                        text = item.label,
                        style = Spent.type.label.copy(fontWeight = FontWeight.SemiBold),
                        color = c.onAc,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

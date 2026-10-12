package pw.shitcode.spent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pw.shitcode.spent.ui.theme.Spent

@Composable
fun SpentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Spent.colors
    SpentText(
        text = text,
        style = Spent.type.label.copy(fontWeight = FontWeight.SemiBold),
        color = c.onAc,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.ac)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    )
}

/** Кнопка с обводкой для второстепенных действий (экспорт). Пока enabled = false, не нажимается и тускнеет. */
@Composable
fun SpentOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Spent.colors
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, c.bar, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        SpentText(
            text = text,
            style = Spent.type.label.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) c.tx else c.tv,
            maxLines = 1,
        )
    }
}

package pw.shitcode.spent.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pw.shitcode.spent.ui.theme.Spent

/** Пустое состояние «нет доступа»: штриховое кольцо, замок, пояснение и кнопка. */
@Composable
fun EmptyAccess(
    title: String,
    body: String,
    button: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Spent.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(84.dp)) {
                drawCircle(
                    color = c.bar,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                    ),
                )
            }
            SpentIcon(IconId.Lock, tint = c.tv, size = 30.dp)
        }
        SpentText(
            text = title,
            style = Spent.type.h1.copy(fontSize = 20.sp, lineHeight = 26.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        SpentText(
            text = body,
            color = c.tv,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
        )
        SpentButton(text = button, onClick = onGrant)
    }
}

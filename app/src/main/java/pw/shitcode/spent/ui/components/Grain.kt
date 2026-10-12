package pw.shitcode.spent.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import pw.shitcode.spent.ui.theme.Spent
import kotlin.random.Random

/** Зернистая текстура поверх экрана. Касания не перехватывает. */
@Composable
fun Grain(modifier: Modifier = Modifier) {
    val dark = Spent.colors.dark
    val brush = remember {
        val side = 128
        val rnd = Random(7)
        val px = IntArray(side * side) {
            val v = rnd.nextInt(256)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        val bmp = Bitmap.createBitmap(px, side, side, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bmp.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(brush, alpha = if (dark) 0.07f else 0.05f)
    }
}

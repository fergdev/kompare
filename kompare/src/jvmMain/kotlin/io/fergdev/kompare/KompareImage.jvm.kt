package io.fergdev.kompare

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

internal actual fun createDiffImage(
    width: Int,
    height: Int,
    diffMask: ByteArray,
): ImageBitmap {
    val highlightColor = 0xFFFFFFFF.toInt()
    val transparent    = 0x00000000

    val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    var i = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (diffMask[i].toInt() != 0) {
                println("setting pixel $x, $y")
            }
            val color = if (diffMask[i].toInt() != 0) highlightColor else transparent
            bi.setRGB(x, y, color)
            i++
        }
    }

    return bi.toComposeImageBitmap()
}
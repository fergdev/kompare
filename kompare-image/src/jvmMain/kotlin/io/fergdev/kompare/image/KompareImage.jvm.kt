package io.fergdev.kompare.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

internal actual fun ImageBitmap.readPixelsByteArray(): ByteArray? {
    val awtImage = this.toAwtImage()

    val bufferedImage = if (awtImage.type == BufferedImage.TYPE_INT_ARGB) {
        awtImage
    } else {
        val newBufferedImage = BufferedImage(
            awtImage.getWidth(null),
            awtImage.getHeight(null),
            BufferedImage.TYPE_INT_ARGB // Ensures 4 bytes per pixel (A,R,G,B)
        )
        val g = newBufferedImage.createGraphics()
        g.drawImage(awtImage, 0, 0, null)
        g.dispose()
        newBufferedImage
    }

    return try {
        // Get pixels as an IntArray (ARGB)
        val pixels = IntArray(bufferedImage.width * bufferedImage.height)
        bufferedImage.getRGB(0, 0, bufferedImage.width, bufferedImage.height, pixels, 0, bufferedImage.width)

        // Convert IntArray (ARGB) to ByteArray
        val byteBuffer = java.nio.ByteBuffer.allocate(pixels.size * 4) // 4 bytes per int
        byteBuffer.asIntBuffer().put(pixels)
        byteBuffer.array()
    } catch (e: IllegalStateException) {
        System.err.println("Error reading pixels: ${e.message}")
        null
    }
}

public actual fun createDiffImage(
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

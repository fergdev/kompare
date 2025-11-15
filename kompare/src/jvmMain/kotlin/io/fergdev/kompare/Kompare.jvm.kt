package io.fergdev.kompare

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.imageio.ImageIO

internal fun ImageBitmap.toByteArray(): ByteArray? {
    // quality is typically for JPEG, ImageIO handles it if applicable for the format.
    val awtImage = this.toAwtImage() // Converts Compose ImageBitmap to java.awt.Image
    val bufferedImage = java.awt.image.BufferedImage(
        awtImage.getWidth(null),
        awtImage.getHeight(null),
        java.awt.image.BufferedImage.TYPE_INT_ARGB // Or infer based on image
    )
    val g2d = bufferedImage.createGraphics()
    g2d.drawImage(awtImage, 0, 0, null)
    g2d.dispose()

    val stream = ByteArrayOutputStream()
    val success = ImageIO.write(bufferedImage, "png", stream) // format.name will be "PNG" or "JPEG"
    return if (success) {
        stream.toByteArray()
    } else {
        null
    }
}

internal actual suspend fun saveFile(
    imageData: ImageBitmap,
    fileName: String,
    directory: String? // e.g., "MyAppImages" for subfolder in user's Pictures or custom path
): String? = withContext(Dispatchers.IO) {
    try {
        // Determine base directory
        val baseDir = if (directory != null) {
            File(directory)
        } else {
            // Default to user's Pictures directory
            val picturesPath = System.getProperty("user.home") + File.separator + "Pictures"
            File(picturesPath)
        }

        // Ensure the base directory and any app-specific subfolder exist
        val targetDir = if (directory != null && directory.contains(File.separator)) {
            baseDir // Assume 'directory' is a full or relative path including subfolders
        } else if (directory != null) {
            File(baseDir, directory) // Treat 'directory' as a subfolder name
        } else {
            File(baseDir, "MyAppName") // Default subfolder if no directory specified
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val file = File(targetDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(imageData.toByteArray())
        }
        return@withContext file.absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        return@withContext null
    }
}

internal actual fun ImageBitmap.readPixelsByteArray(): ByteArray? {
    val awtImage = this.toAwtImage() // Converts Compose ImageBitmap to java.awt.Image

    // Ensure we have a BufferedImage to work with
    val bufferedImage = if (awtImage is BufferedImage && awtImage.type == BufferedImage.TYPE_INT_ARGB) {
        awtImage
    } else {
        // Convert to a standard format like TYPE_INT_ARGB for consistent pixel layout
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

    // Accessing raw pixel data from BufferedImage
    // This method gets data directly if the internal buffer is byte-based and matches our expectation.
    // For TYPE_INT_ARGB, the raster's data buffer is usually DataBufferInt.
    // A more robust way might be to get pixels into an IntArray then convert to ByteArray.

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

    // Alternative (less direct for raw pixel comparison, but good for visual similarity):
    // Convert to a canonical format like PNG and compare bytes.
    // This handles different internal representations but adds encoding overhead.
    // try {
    //     ByteArrayOutputStream().use { outputStream ->
    //         ImageIO.write(bufferedImage, "PNG", outputStream)
    //         return outputStream.toByteArray()
    //     }
    // } catch (e: Exception) {
    // e.printStackTrace()
    // return null
    // }
}

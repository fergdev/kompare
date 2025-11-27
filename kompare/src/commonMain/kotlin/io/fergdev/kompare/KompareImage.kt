package io.fergdev.kompare

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.max
import kotlin.test.assertTrue


public suspend fun KompareScope.kompareImage(
    reader: KReader,
    testNameResolver: TestNameResolver,
) {
//        this.kompare(reader, testNameResolver)
    val actual = this.graphicsLayer.toImageBitmap()
    saveFile(actual, testNameResolver.getFullTestName() + ".png")
    val path = "files/kompare/" + testNameResolver.getFullTestName() + ".png"
    val expected = reader.readBytes(path).decodeToImageBitmap()
//        assertTrue(compareImageBitmaps(expected, actual))
    val isEqual = compareImageBitmaps(expected, actual)
    if (!isEqual) {
        val diff = generateImageDiff(expected, actual)
        saveFile(diff, testNameResolver.getFullTestName() + "_diff.png")
    }

    assertTrue(isEqual)
}

/**
 * Compares two ImageBitmaps and returns a new ImageBitmap highlighting the differences.
 *
 * This function requires the `org.jetbrains.compose.ui:ui-desktop` dependency for the
 * `.toAwtImage()` and `.toComposeImageBitmap()` conversion functions.
 *
 * @param golden The baseline or "golden" image.
 * @param new The new image to compare against the golden image.
 * @return An ImageBitmap where differing pixels are colored red and matching pixels are transparent.
 */
private fun generateImageDiff(golden: ImageBitmap, new: ImageBitmap): ImageBitmap {
    // 1. Convert Compose ImageBitmaps to AWT BufferedImages for pixel access.
    val goldenImage = golden.toPixelMap()
    val newImage = new.toPixelMap()

    // 2. Determine the dimensions for the output diff image.
    val width = max(golden.width, new.width)
    val height = max(golden.height, new.height)

    // 3. Create a new transparent BufferedImage to draw the diff onto.
    val diffBytes = ByteArray(width * height)
//    val diffImage = ImageBitmap(width, height, ImageBitmapConfig.Argb8888)
//    diffImage.readPixelsByteArray()
//    val diffImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val highlightColor = androidx.compose.ui.graphics.Color.Red


    // 4. Iterate through each pixel to find differences.
    for (y in 0 until height) {
        for (x in 0 until width) {
            val goldenPixel = if (x < golden.width && y < golden.height) {
                goldenImage[x, y].value
            } else {
                0 as ULong // Treat out-of-bounds pixels as transparent black
            }

            val newPixel = if (x < new.width && y < new.height) {
                newImage[x, y].value
            } else {
                0 as ULong
            }

            // 5. If the pixels are different, color the diff image red.
            if (goldenPixel != newPixel) {
                diffBytes[y * width + x] = highlightColor.toArgb().toByte()
            }
        }
    }

    // 6. Convert the resulting AWT image back to a Compose ImageBitmap.
//    return diffImage.toComposeImageBitmap()
    return diffBytes.decodeToImageBitmap()
}


internal expect fun ImageBitmap.readPixelsByteArray(): ByteArray?

//internal fun compare(
//    bitmap1: ImageBitmap?, bitmap2: ImageBitmap?
//): Boolean {
//    if (bitmap1 == null && bitmap2 == null) return true // Both null, considered equal
//    if (bitmap1 == null || bitmap2 == null) return false // One is null, other isn't
//    val differ = SimpleImageComparator()
//    val result = differ.compare(DifferImage(bitmap1), DifferImage(bitmap2))
//    return result.pixelDifferences == 0
//}
//
//private class DifferImage(
//    val ib: ImageBitmap
//) : Image {
//    private val pixelMap = ib.toPixelMap()
//    override val height: Int
//        get() = ib.height
//    override val width: Int
//        get() = ib.width
//
//    override fun getPixel(x: Int, y: Int): Color {
//        val at = pixelMap[x, y]
//        return Color(at.red, at.green, at.blue, at.alpha)
//    }
//}

internal fun compareImageBitmaps(bitmap1: ImageBitmap?, bitmap2: ImageBitmap?): Boolean {
    println("compareImageBitmaps")
    if (bitmap1 == null && bitmap2 == null) return true // Both null, considered equal
    if (bitmap1 == null || bitmap2 == null) return false // One is null, other isn't
    if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
        return false
    }

    val pixels1 = bitmap1.readPixelsByteArray()
    val pixels2 = bitmap2.readPixelsByteArray()

    if (pixels1 == null || pixels2 == null) {
        return false
    }

    return pixels1.contentEquals(pixels2)
}

/**
 * Saves image data to a file.
 * @param imageData The raw byte array of the image.
 * @param fileName The suggested file name (e.g., "my_image.png"). Platform implementations
 *                 might adjust this or place it in specific directories.
 * @param directory (Optional) A suggested directory or sub-path. Interpretation varies by platform.
 *                  For public shared images, this might be ignored in favor of platform conventions
 *                  (e.g., Pictures folder on Android/Desktop, Photos library on iOS).
 * @return The path or URI to the saved file as a String, or null if saving failed.
 */
internal expect suspend fun saveFile(
    imageData: ImageBitmap,
    fileName: String,
    directory: String? = null
): String?


package io.fergdev.kompare.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlin.math.max

public sealed interface DiffResult {

    public data class SizeDiff(
        val expectedWidth: Int,
        val expectedHeight: Int,
        val actualWidth: Int,
        val actualHeight: Int,
        val diffImage: ImageBitmap,
    ) : DiffResult

    public data class PixelDiff(
        val diffImage: ImageBitmap,
    ) : DiffResult

    public data object NoDiff : DiffResult
    public data class Error(val message: String) : DiffResult
}

public fun diffImage(
    expected: ImageBitmap,
    actual: ImageBitmap
): DiffResult {
    if (expected.width != actual.width || expected.height != actual.height) {
        return DiffResult.SizeDiff(
            expectedWidth = expected.width,
            expectedHeight = expected.height,
            actualWidth = actual.width,
            actualHeight = actual.height,
            diffImage = genDiff(expected, actual)
        )
    }

    val expectedPixels = expected.readPixelsByteArray()
    val actualPixels = actual.readPixelsByteArray()

    if (expectedPixels == null || actualPixels == null) {
        return DiffResult.Error("Error reading pixels")
    }
    if (!expectedPixels.contentEquals(actualPixels)) {
        return DiffResult.PixelDiff(
            diffImage = genDiff(expected, actual)
        )

    }
    return DiffResult.NoDiff
}

private fun genDiff(
    golden: ImageBitmap,
    new: ImageBitmap,
): ImageBitmap {
    val goldenImage = golden.toPixelMap()
    val newImage = new.toPixelMap()
    val width = max(golden.width, new.width)
    val height = max(golden.height, new.height)
    val diffBytes = ByteArray(width * height)

    var i = 0
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

            if (goldenPixel != newPixel) {
                diffBytes[i] = 1
            }
            i++
        }
    }
    return createDiffImage(width, height, diffBytes)
}

internal expect fun ImageBitmap.readPixelsByteArray(): ByteArray?

public expect fun createDiffImage(
    width: Int,
    height: Int,
    diffMask: ByteArray,
): ImageBitmap

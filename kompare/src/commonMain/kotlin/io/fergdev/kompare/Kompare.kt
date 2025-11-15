package io.fergdev.kompare

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
private fun SemanticsNodeInteractionsProvider.serializeDomToNode() =
    onRoot().fetchSemanticsNode().serializeDomNode()

private fun SemanticsNode.serializeDomNode(): KNode {
    val childrenNode = children.map { it.serializeDomNode() }
    return KNode(
        this.id,
        this.unclippedGlobalBounds,
        this.config.toPropertyMap(),
        childrenNode
    )
}

private fun SemanticsConfiguration.toPropertyMap() =
    mutableMapOf<String, String>().apply {
        this@toPropertyMap.forEach {
            this@apply.put(
                it.key.toString(),
                it.value.toString()
                    .replace(Regex("\\$\\\$Lambda.*"), "")
                    .replace(Regex("\\$\\\$ExternalSyntheticLambda.*"), "")
            )
        }
    }

@Suppress("ExpressionBodySyntax")
private val SemanticsNode.unclippedGlobalBounds: Rect
    get() {
        return createRect(positionInWindow, size.toSize())
    }

@Serializable
internal data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private const val INVALID_SEMANTIC_NODE_BOUNDS = "Invalid Semantic Node bounds"

private fun Rect.assertWithin(other: Rect, slop: Float = 5.0f) {
    assertTrue(
        (left - other.left).absoluteValue < slop,
        message = "$INVALID_SEMANTIC_NODE_BOUNDS for left ${this.left} != ${other.left}"
    )
    assertTrue(
        (right - other.right).absoluteValue < slop,
        message = "$INVALID_SEMANTIC_NODE_BOUNDS for right ${this.right} != ${other.right}"
    )
    assertTrue(
        (top - other.top).absoluteValue < slop,
        message = "$INVALID_SEMANTIC_NODE_BOUNDS for top ${this.top} != ${other.top}"
    )
    assertTrue(
        (bottom - other.bottom).absoluteValue < slop,
        message = "$INVALID_SEMANTIC_NODE_BOUNDS for bottom ${this.bottom} != ${other.bottom}"
    )
}

private fun createRect(offset: Offset, size: Size) =
    Rect(
        offset.x,
        offset.y,
        offset.x + size.width,
        offset.y + size.height
    )

public interface KReader {
    public suspend fun readBytes(path: String): ByteArray
}

public interface TestNameResolver {
    public fun getFullTestName(): String
}

@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalTestApi::class)
public suspend fun SemanticsNodeInteractionsProvider.kompare(
    reader: KReader,
    testNameResolver: TestNameResolver,
) {
    val actualKTree = serializeDomToNode()
    val path = "files/kompare/" + testNameResolver.getFullTestName() + ".json"
    val expected = try {
        reader.readBytes(path).decodeToString()
    } catch (t: Throwable) {
        println("Unable to load path '$path'")
        println("Possible missing test data???")
        println("Add following to complete test")
        println("Path '$path'")
        println("Data '${Json.encodeToString(actualKTree)}'")
        throw t
    }
    require(expected.isNotBlank()) {
        "Kompare: empty file at path '$path'" +
                "Actual tree '${Json.encodeToString(actualKTree)}'"
    }
    try {
        val expectedKTree = Json.decodeFromString<KNode>(expected)
        walk(expectedKTree, actualKTree)
    } catch (assertionFailedError: Throwable) {
        println("expected $expected")
        println("actual ${Json.encodeToString(actualKTree)}")
        throw assertionFailedError
    }
}

private fun walk(kna: KNode, knb: KNode) {
    kna.bounds.assertWithin(knb.bounds)

    for (entry in kna.properties) {
        assertEquals(
            entry.value,
            knb.properties[entry.key],
            message = "Invalid Semantic Node properties"
        )
    }
    assertEquals(kna.properties.size, knb.properties.size)

    kna.children.zip(knb.children).forEach {
        walk(it.first, it.second)
    }
}

@OptIn(ExperimentalTestApi::class)
public fun runKompareUiTest(
    effectContext: CoroutineContext = EmptyCoroutineContext,
    runTestContext: CoroutineContext = EmptyCoroutineContext,
    testTimeout: Duration = 60.seconds,
    block: suspend KompareScope.() -> Unit
) {
    runComposeUiTest(effectContext, runTestContext, testTimeout) {
        val scope = KompareScope(this)
        with(scope) {
            block()
        }
    }
}

@OptIn(ExperimentalTestApi::class)
public class KompareScope(
    private val composeUiTest: ComposeUiTest
) : SemanticsNodeInteractionsProvider by composeUiTest {

    public val density: Density = composeUiTest.density
    public val mainClock: MainTestClock = composeUiTest.mainClock
    public fun <T> runOnUiThread(action: () -> T): T = composeUiTest.runOnUiThread(action)

    public fun <T> runOnIdle(action: () -> T): T = composeUiTest.runOnIdle(action)
    public fun waitForIdle(): Unit = composeUiTest.waitForIdle()
    public suspend fun awaitIdle(): Unit = composeUiTest.awaitIdle()
    public fun waitUntil(
        conditionDescription: String?,
        timeoutMillis: Long,
        condition: () -> Boolean
    ): Unit = composeUiTest.waitUntil(conditionDescription, timeoutMillis, condition)

    @Suppress("LateinitUsage")
    private lateinit var graphicsLayer: GraphicsLayer

    public fun setContent(composable: @Composable () -> Unit) {
        composeUiTest.setContent {
            this.graphicsLayer = rememberGraphicsLayer()
            Box(
                modifier = Modifier
                    .drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
            ) {
                composable()
            }
        }
    }

    public suspend fun doKompare(
        reader: KReader,
        testNameResolver: TestNameResolver,
    ) {
        this.kompare(reader, testNameResolver)
        val actual = this.graphicsLayer.toImageBitmap()
        saveFile(actual, testNameResolver.getFullTestName() + ".png")
        val path = "files/kompare/" + testNameResolver.getFullTestName() + ".png"
        val expected = reader.readBytes(path).decodeToImageBitmap()
        assertEquals(expected.width, actual.width)
        assertEquals(expected.height, actual.height)
        compareImageBitmaps(expected, actual)
    }
}

internal expect fun ImageBitmap.readPixelsByteArray(): ByteArray?

internal fun compareImageBitmaps(bitmap1: ImageBitmap?, bitmap2: ImageBitmap?): Boolean {
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

// commonMain/kotlin/your/package/FuzzyImageComparator.common.kt
// (This file would provide a common implementation if no `expect fun areImagesSimilar` is used,
// or it could be part of the `actual` implementation if `expect` is used but shares logic)
// commonMain/kotlin/your/package/FuzzyImageComparator.kt
// import androidx.compose.ui.graphics.ImageBitmap
// enum class ComparisonAlgorithm {
//    PIXEL_DIFF_PERCENTAGE, // Simple percentage of differing pixels
//    MSE,                   // Mean Squared Error
//    SSIM                   // Structural Similarity Index (more complex)
// }
//
// data class ComparisonResult(
//    val areSimilar: Boolean,
//    val differenceMetric: Double, // e.g., MSE value, percentage of different pixels, SSIM score
//    val algorithmUsed: ComparisonAlgorithm
// )
//
// /**
// * Expected function for fuzzy image comparison.
// * Implementations will handle pixel data extraction and comparison.
// *
// * @param bitmap1 The first image.
// * @param bitmap2 The second image.
// * @param algorithm The algorithm to use for comparison.
// * @param tolerance For PIXEL_DIFF_PERCENTAGE, this is the max allowed percentage of differing pixels (0.0 to 1.0).
// *                  For MSE, this could be the max allowed MSE value.
// *                  For SSIM, this is typically the min required SSIM score (close to 1.0 means very similar).
// * @param pixelDifferenceThreshold For PIXEL_DIFF_PERCENTAGE, the max difference for a single channel (0-255)
// *                                 or overall pixel color difference for pixels to be considered "same".
// * @return ComparisonResult indicating similarity and the calculated metric.
// */
// expect fun areImagesSimilar(
//    bitmap1: ImageBitmap,
//    bitmap2: ImageBitmap,
//    algorithm: ComparisonAlgorithm = ComparisonAlgorithm.PIXEL_DIFF_PERCENTAGE,
//    tolerance: Double = 0.05, // Default: 5% difference allowed for PIXEL_DIFF_PERCENTAGE
//    pixelDifferenceThreshold: Int = 10 // Default: Max channel diff of 10 for PIXEL_DIFF_PERCENTAGE
// ): ComparisonResult?

// Assuming ImageBitmap.readPixelsByteArray() expect/actual is available from previous discussions
// import your.package.readPixelsByteArray

// Common implementation for PIXEL_DIFF_PERCENTAGE
// This would be called by your `actual fun areImagesSimilar` or be the direct common implementation.
// fun calculatePixelDifferencePercentage(
//    bitmap1: ImageBitmap,
//    bitmap2: ImageBitmap,
//    pixelDifferenceThreshold: Int // Max difference per channel (0-255) for pixels to be "same"
// ): Double? { // Returns percentage of different pixels (0.0 to 1.0), or null on error
//    if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
//        return 1.0 // Treat as 100% different if dimensions mismatch for this simple algo
//    }
//
//    val pixels1Bytes = bitmap1.readPixelsByteArray() // Assumes ARGB_8888 or RGBA_8888 (4 bytes per pixel)
//    val pixels2Bytes = bitmap2.readPixelsByteArray()
//
//    if (pixels1Bytes == null || pixels2Bytes == null || pixels1Bytes.size != pixels2Bytes.size) {
//        return null // Error reading pixels or mismatched data size
//    }
//
//    val numPixels = bitmap1.width * bitmap1.height
//    if (numPixels == 0) return 0.0 // No pixels to compare
//
//    var differingPixelsCount = 0
//    val bytesPerPixel = 4 // Assuming ARGB_8888 or RGBA_8888
//
//    for (i in 0 until numPixels) {
//        val offset = i * bytesPerPixel
//        var differentPixel = false
//        for (byteIndex in 0 until bytesPerPixel) { // Compare each byte (channel) within the pixel
//            // For ARGB, byte 0 might be Alpha, 1=Red, 2=Green, 3=Blue (or BGRA, etc.)
//            // For simplicity, let's just compare absolute byte differences.
//            // A more accurate comparison would unpack to R, G, B, A ints first.
//            // This also assumes that alpha is byte 0 and we might want to ignore it or treat it differently.
//            // Let's assume for this simple example we compare all 4 bytes.
//            // If you only care about RGB, skip the alpha byte or ensure it's handled.
//
//            val diff = kotlin.math.abs(pixels1Bytes[offset + byteIndex].toInt() - pixels2Bytes[offset + byteIndex].toInt())
//            if (diff > pixelDifferenceThreshold) {
//                differentPixel = true
//                break // This pixel is different
//            }
//        }
//        if (differentPixel) {
//            differingPixelsCount++
//        }
//    }
//    return differingPixelsCount.toDouble() / numPixels.toDouble()
// }
//
// // Actual implementation (if using the `expect fun areImagesSimilar`)
// // This would go in commonMain if you're NOT using expect/actual for the areImagesSimilar itself,
// // but rather using the common pixel reading.
// // If areImagesSimilar is expected, this logic moves into each platform's actual.
//
// /*
// actual fun areImagesSimilar(
//    bitmap1: ImageBitmap,
//    bitmap2: ImageBitmap,
//    algorithm: ComparisonAlgorithm,
//    tolerance: Double,
//    pixelDifferenceThreshold: Int
// ): ComparisonResult? {
//    if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
//        // You might want to return a specific ComparisonResult indicating dimension mismatch
//        // For simplicity, let's say they are not similar in this case for any algorithm.
//        return ComparisonResult(false, 1.0, algorithm) // 1.0 can mean 100% different
//    }
//
//    when (algorithm) {
//        ComparisonAlgorithm.PIXEL_DIFF_PERCENTAGE -> {
//            val diffPercentage = calculatePixelDifferencePercentage(bitmap1, bitmap2, pixelDifferenceThreshold)
//                ?: return null // Error in calculation
//            return ComparisonResult(diffPercentage <= tolerance, diffPercentage, algorithm)
//        }
//        ComparisonAlgorithm.MSE -> {
//            // MSE Implementation (see below)
//            val mseValue = calculateMse(bitmap1, bitmap2) ?: return null
//            return ComparisonResult(mseValue <= tolerance, mseValue, algorithm)
//        }
//        ComparisonAlgorithm.SSIM -> {
//            // SSIM is much more complex to implement from scratch in commonMain.
//            // You'd likely use platform libraries via expect/actual for SSIM.
//            println("SSIM not implemented in pure commonMain for this example.")
//            return null
//        }
//    }
// }
// */
//
// // Common MSE calculation (can be in commonMain if pixel reading is common)
// fun calculateMse(bitmap1: ImageBitmap, bitmap2: ImageBitmap): Double? {
//    // Requires pixel data (e.g., from readPixelsByteArray())
//    // Ensure dimensions match first (as done in areImagesSimilar)
//    val pixels1Bytes = bitmap1.readPixelsByteArray() // Assuming ARGB_8888 (4 bytes per pixel)
//    val pixels2Bytes = bitmap2.readPixelsByteArray()
//
//    if (pixels1Bytes == null || pixels2Bytes == null || pixels1Bytes.size != pixels2Bytes.size) {
//        return null
//    }
//    if (bitmap1.width == 0 || bitmap1.height == 0) return 0.0
//
//
//    var sumSquaredError = 0.0
//    val numChannels = 3 // Assuming we compare R, G, B (ignoring Alpha for simplicity or handle it)
//    val bytesPerPixel = 4
//
//    for (i in 0 until (bitmap1.width * bitmap1.height)) {
//        val offset = i * bytesPerPixel
// // Example: Assuming ARGB byte order and we want to compare R, G, B
// // pixels1Bytes[offset + 0] is Alpha
// // pixels1Bytes[offset + 1] is Red
// // pixels1Bytes[offset + 2] is Green
// // pixels1Bytes[offset + 3] is Blue
// // (Byte order can vary based on platform and how readPixelsByteArray is implemented!)
// // It's SAFER to convert to Int pixels and extract channels.
//
// // Safer: Convert to Int an

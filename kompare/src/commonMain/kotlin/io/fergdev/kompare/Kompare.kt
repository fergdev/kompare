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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
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
import kotlin.math.max
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

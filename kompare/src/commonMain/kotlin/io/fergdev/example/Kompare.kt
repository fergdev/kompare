package io.fergdev.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.toSize
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
fun SemanticsNodeInteractionsProvider.serializeDomToNode() =
    onRoot().fetchSemanticsNode().serializeDomNode()

fun SemanticsNode.serializeDomNode(): KNode {
    val childrenNode = children.map { it.serializeDomNode() }
    return KNode(
        this.id,
        this.unclippedGlobalBounds,
        this.config.toPropertyMap(),
        childrenNode
    )
}

fun SemanticsConfiguration.toPropertyMap() =
    mutableMapOf<String, String>().apply {
        this@toPropertyMap.forEach {
// $$Lambda/0x0000000800316c68@5ae76500
            this@apply.put(
                it.key.toString(),
                it.value.toString().replace(Regex("\\$\\\$Lambda.*"), "")
            )
        }
    }

@Suppress("ExpressionBodySyntax")
val SemanticsNode.unclippedGlobalBounds: Rect
    get() {
        return createRect(positionInWindow, size.toSize())
    }

@Serializable
data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private const val INVALID_SEMANTIC_NODE_BOUNDS = "Invalid Semantic Node bounds"

internal fun Rect.assertWithin(other: Rect, slop: Float = 2.0f) {
    assertTrue(left - slop < other.left, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(left + slop > other.left, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(right - slop < other.right, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(right + slop > other.right, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(top - slop < other.top, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(top + slop > other.top, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(bottom - slop < other.bottom, message = INVALID_SEMANTIC_NODE_BOUNDS)
    assertTrue(bottom + slop > other.bottom, message = INVALID_SEMANTIC_NODE_BOUNDS)
}

fun createRect(offset: Offset, size: Size) =
    Rect(
        offset.x,
        offset.y,
        offset.x + size.width,
        offset.y + size.height
    )

interface KReader {
    suspend fun readBytes(path: String): ByteArray
}

interface TestNameResolver {
    fun getFullTestName(): String
}

@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalTestApi::class)
suspend fun SemanticsNodeInteractionsProvider.kompare(
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
        assertEquals(entry.value, knb.properties[entry.key], message = "Invalid Semantic Node properties")
    }
    assertEquals(kna.properties.size, knb.properties.size)

    kna.children.zip(knb.children).forEach {
        walk(it.first, it.second)
    }
}

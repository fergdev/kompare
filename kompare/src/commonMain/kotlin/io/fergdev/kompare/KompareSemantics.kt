package io.fergdev.kompare

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
import kotlin.math.absoluteValue
import kotlin.test.assertTrue

@Suppress("TooGenericExceptionCaught")
@OptIn(ExperimentalTestApi::class)
public suspend fun SemanticsNodeInteractionsProvider.kompare(
    reader: KReader,
    testNameResolver: TestNameResolver,
) {
    val actualKTree = serializeDomToNode()
    val path = "files/kompare/" + testNameResolver.getFullTestName() + ".json"
    val expected = try {
        reader.readBytes(path).decodeToString().trim()
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
        val cxt = WalkContext()
        walk(cxt, expectedKTree, actualKTree)
        val res = cxt.doReport()
        assertTrue(res)
    } catch (assertionFailedError: Throwable) {
        println("expected '\n$expected\n'")
        println("actual '\n${KJson.encodeToString(actualKTree)}\n'")
        throw assertionFailedError
    }
}


private val KJson = Json { prettyPrint = true }

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

//private const val INVALID_SEMANTIC_NODE_BOUNDS = "Invalid Semantic Node bounds"

private fun Rect.assertWithin2(other: Rect, slop: Float = 5.0f) =
    (left - other.left).absoluteValue < slop ||
            (right - other.right).absoluteValue < slop ||
            (top - other.top).absoluteValue < slop ||
            (bottom - other.bottom).absoluteValue < slop

//private fun Rect.assertWithin(other: Rect, slop: Float = 5.0f) {
//    assertTrue(
//        (left - other.left).absoluteValue < slop,
//        message = "$INVALID_SEMANTIC_NODE_BOUNDS for left ${this.left} != ${other.left}"
//    )
//    assertTrue(
//        (right - other.right).absoluteValue < slop,
//        message = "$INVALID_SEMANTIC_NODE_BOUNDS for right ${this.right} != ${other.right}"
//    )
//    assertTrue(
//        (top - other.top).absoluteValue < slop,
//        message = "$INVALID_SEMANTIC_NODE_BOUNDS for top ${this.top} != ${other.top}"
//    )
//    assertTrue(
//        (bottom - other.bottom).absoluteValue < slop,
//        message = "$INVALID_SEMANTIC_NODE_BOUNDS for bottom ${this.bottom} != ${other.bottom}"
//    )
//}

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

private class WalkContext {
    private val prefix = "-"
    private val errors = mutableListOf<Pair<String, WalkError>>()
    private val sb = StringBuilder("")

    fun report(error: WalkError) {
        errors.add(sb.toString() to error)
    }

    fun visit(node: KNode) {
        sb.append(prefix + node.id)
    }

    fun unVisit(node: KNode) {
        sb.removeSuffix(prefix + node.id)
    }

    fun doReport(): Boolean {
        if (errors.isNotEmpty()) {
            errors.forEach {
                println(it.first + " - " + it.second.toErrorString())
            }
        }
        return errors.isEmpty()
    }
}

private sealed class WalkError {
    abstract fun toErrorString(): String
    data class InvalidSemanticNodeBounds(val node: KNode, val other: KNode) : WalkError() {
        override fun toErrorString(): String =
            "Invalid Semantic Node bounds ${node.bounds}, ${other.bounds}"
    }

    data class PropertyMismatch(val node: KNode, val other: KNode, val key: String) : WalkError() {
        override fun toErrorString(): String =
            "Invalid Semantic Node properties key '$key' ${node.properties[key]} - ${other.properties[key]}"
    }
}

private fun walk(
    cxt: WalkContext,
    kna: KNode,
    knb: KNode
) {
    cxt.visit(kna)
    if (!kna.bounds.assertWithin2(knb.bounds)) {
        cxt.report(WalkError.InvalidSemanticNodeBounds(kna, knb))
    }
//    kna.bounds.assertWithin(knb.bounds)

    val knbProps = knb.properties.keys.toMutableSet()
    for (entry in kna.properties) {
        if (entry.value != knb.properties[entry.key]) {
            cxt.report(WalkError.PropertyMismatch(kna, knb, entry.key))
        }
        knbProps.remove(entry.key)
//        assertEquals(
//            entry.value,
//            knb.properties[entry.key],
//            message = "Invalid Semantic Node properties"
//        )
    }
//    assertEquals(kna.properties.size, knb.properties.size)
    if (knb.properties.isNotEmpty()) {
        knbProps.forEach {
            cxt.report(WalkError.PropertyMismatch(kna, knb, it))
        }
    }

    kna.children.zip(knb.children).forEach {
        walk(cxt, it.first, it.second)
    }
    cxt.unVisit(kna)
}


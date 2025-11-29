package io.fergdev.kompare

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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
    internal lateinit var graphicsLayer: GraphicsLayer

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
}

package io.fergdev.kompare.example

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.fergdev.kompare.KReader
import io.fergdev.kompare.TestNameResolver
import io.fergdev.kompare.kompareImage
import io.fergdev.kompare.kompare
import io.fergdev.kompare.runKompareUiTest
import kompareproj.sample_kmp.shared.generated.resources.Res
import kotlin.test.Test

class ComposeAppJvmTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun example() {
        runComposeUiTest {
            setContent { App() }

            kompare(
                testNameResolver = object : TestNameResolver {
                    override fun getFullTestName(): String = "example"
                },
                reader = RunContentTestReader
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun example2() {
        runComposeUiTest {
            setContent { App() }
            onNodeWithText("Click me!").performClick()
            kompare(
                testNameResolver = object : TestNameResolver {
                    override fun getFullTestName(): String = "example2"
                },
                reader = RunContentTestReader
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun example3() =
        runKompareUiTest {
            setContent { App() }
            onNodeWithText("Click me!").performClick()
            awaitIdle()
            kompareImage(
                testNameResolver = object : TestNameResolver {
                    override fun getFullTestName(): String = "example3"
                },
                reader = RunContentTestReader
            )
        }
}

object RunContentTestReader : KReader {
    override suspend fun readBytes(path: String) = Res.readBytes(path)
}

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.fergdev.example.KReader
import io.fergdev.example.TestNameResolver
import io.fergdev.example.kompare
import io.fergdev.kompare.example.App
import kompareproj.composeapp.generated.resources.Res
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ComposeAppAndroidUnitTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun example() = runTest {
        runComposeUiTest {
            setContent {
                App()
            }

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
    fun example2() = runTest {
        runComposeUiTest {
            setContent {
                App()
            }
            onNodeWithText("Click me!").performClick()
            kompare(
                testNameResolver = object : TestNameResolver {
                    override fun getFullTestName(): String = "example2"
                },
                reader = RunContentTestReader
            )
        }
    }
}

object RunContentTestReader : KReader {
    override suspend fun readBytes(path: String) = Res.readBytes(path)
}

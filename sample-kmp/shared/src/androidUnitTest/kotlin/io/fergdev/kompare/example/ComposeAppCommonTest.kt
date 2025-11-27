package io.fergdev.kompare.example

import io.fergdev.kompare.KReader
import kompareproj.sample_kmp.generated.resources.Res

class ComposeAppCommonTest {

//    @OptIn(ExperimentalTestApi::class)
//    @org.junit.Test
//    fun wowow() {
//        runComposeUiTest {
//            setContent {
//                Text(text = "Hello")
//            }
//            onNodeWithText("Hello").assertExists()
//            onNodeWithText("Hello1").assertDoesNotExist()
//        }
//    }

//    @OptIn(ExperimentalTestApi::class)
//    @JvmField
//    @RegisterExtension
//    val extension = createComposeExtension()
//
//    @JvmField
//    @RegisterExtension
//    val scenarioExtension = ActivityScenarioExtension.launch<MainActivity>()
//
//    @Test
//    fun wow() {
//        scenarioExtension.scenario.onActivity {
//            assert(false)
//        }
//    }
//
//    @Test
//    fun example() = extension.use {
//        setContent { App() }
//        onNodeWithText("Click me!").performClick()
//        onNodeWithText("helllllooo").assertExists()
//    }
}

object RunContentTestReader : KReader {
    override suspend fun readBytes(path: String) = Res.readBytes(path)
}

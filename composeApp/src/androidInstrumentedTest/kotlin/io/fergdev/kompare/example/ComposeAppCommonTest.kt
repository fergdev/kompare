package io.fergdev.kompare.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.fergdev.example.KReader
import kompareproj.composeapp.generated.resources.Res
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
//@Config(instrumentedPackages = ["androidx.loader.content"])

class ComposeAppCommonTestInst {

    @get:Rule
    val testRule = createAndroidComposeRule<HostActivity>()

//    val testRule = createComposeRule()

    @Test
    fun example() {
        testRule.setContent { App() }
//        GlobalScope.launch {
//            with(testRule) {
//                kompare(
//                    testNameResolver = object : TestNameResolver {
//                        override fun getFullTestName(): String = "example"
//                    },
//                    reader = RunContentTestReader
//                )
//            }
//        }
        testRule.onNodeWithTag("hi").assertExists()
    }
}

object RunContentTestReader : KReader {
    override suspend fun readBytes(path: String) = Res.readBytes(path)
}

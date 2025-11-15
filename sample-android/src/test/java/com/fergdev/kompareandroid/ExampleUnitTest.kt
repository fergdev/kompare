package com.fergdev.kompareandroid

import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.fergdev.kompare.KReader
import io.fergdev.kompare.TestNameResolver
import io.fergdev.kompare.runKompareUiTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [30],
    qualifiers = io.fergdev.kompare.RobolectricDeviceQualifiers.NexusOne,
    application = TestApplication::class,
)
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        val cl = javaClass.classLoader!!
        runKompareUiTest {
            setContent {
                Text(text = "hi")
            }
            delay(1000)
            awaitIdle()
            doKompare(
                reader = object : KReader {
                    override suspend fun readBytes(path: String): ByteArray =
                        cl.getResourceAsStream(path.substringAfterLast("/")).readBytes()
                },
                testNameResolver = object : TestNameResolver {
                    override fun getFullTestName(): String = "test"
                },
            )
        }
        assertEquals(4, 2 + 2)
    }
}

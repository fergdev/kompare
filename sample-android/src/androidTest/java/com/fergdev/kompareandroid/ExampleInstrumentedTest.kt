package com.fergdev.kompareandroid

import androidx.compose.material3.Text
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.fergdev.kompare.KReader
import io.fergdev.kompare.TestNameResolver
import io.fergdev.kompare.runKompareUiTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.IOException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    // Doesn't work, see roborazzi for possible implementation.
//    @Test
    fun useAppContext() {
        runKompareUiTest {
            setContent {
                Text(text = "Hi")
            }
            doKompare(
                reader = object : KReader {
                    override suspend fun readBytes(path: String): ByteArray {
                        val instrumentation = InstrumentationRegistry.getInstrumentation()
                        // Use the context of the test APK itself to find resources declared in src/androidTest/res
                        val testContext = instrumentation.context
                        val resources = testContext.resources

                        val resourceSuffix = path.substringAfterLast('.')
                        val resourceName = path.substringBeforeLast('.')
                            .replace("files/kompare/", "") + "_" + resourceSuffix
                        val resourceId = resources.getIdentifier(
                            resourceName,
                            "raw", // Look in the 'raw' directory
                            testContext.packageName
                        )
                        if (resourceId == 0) {
                            throw IOException("Resource not found for raw/$resourceName in package ${testContext.packageName}")
                        }
                        return resources.openRawResource(resourceId).use { inputStream ->
                            inputStream.readBytes()
                        }
                    }
                },
                testNameResolver = object : TestNameResolver {
                    override fun getFullTestName(): String {
                        return "test"
                    }
                }
            )
        }
    }
}
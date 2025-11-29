package io.fergdev.kompare.kotest

import io.kotest.core.test.TestCase
import io.kotest.core.test.TestScope

public fun TestScope.getFullTestName(): String {
    val specName = this.testCase.spec::class.simpleName ?: "unknown"
    val list = mutableListOf<String>()
    var testCase: TestCase? = this.testCase
    while (testCase != null) {
        list.add(testCase.name.name)
        testCase = testCase.parent
    }
    list.add(specName)
    list.reverse()
    return list.joinToString("-")
}

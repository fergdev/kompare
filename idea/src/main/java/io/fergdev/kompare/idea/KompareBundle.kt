package io.fergdev.kompare.idea

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
private const val BUNDLE = "messages.KompareBundle"

public object KompareBundle : DynamicBundle(BUNDLE) {

    @JvmStatic
    public fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): String = getMessage(key, *params)

    @Suppress("unused")
    @JvmStatic
    public fun messagePointer(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): Supplier<String> =
        getLazyMessage(key, *params)
}

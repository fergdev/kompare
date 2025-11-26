package io.fergdev.kompare.example

class AndroidPlatform : Platform {
//    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val name: String = "Android"
}

actual fun getPlatform(): Platform = AndroidPlatform()

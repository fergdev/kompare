package io.fergdev.kompare.example

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

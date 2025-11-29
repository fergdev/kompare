@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)
}

kotlin {
    explicitApi()
    androidLibrary {
        namespace = "io.fergdev.kompare.image"
        compileSdk = 36
        minSdk = 24
    }
    jvm()
    js {
        browser()
        nodejs()
        binaries.library()
    }
    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    val xcfName = "kompare-imageKit"
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = xcfName
        }
    }
    sourceSets {
        commonMain {
            dependencies {
//                implementation(libs.kotlin.stdlib)
                implementation(libs.composeUi)
//                implementation(libs.composeFoundation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
            }
        }
        iosMain {
            dependencies {
            }
        }
    }
}
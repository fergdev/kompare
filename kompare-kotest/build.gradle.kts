plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)
    id(libs.plugins.androidLint.get().pluginId)
}

kotlin {
    androidLibrary {
        namespace = "${Config.artifactId}.kotest"
        compileSdk = Config.Android.compileSdk
        minSdk = Config.Android.minSdk

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    val xcfName = "${Config.name}-kotestKit"
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())
        .forEach {
            it.binaries.framework {
                baseName = xcfName
            }
        }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotest)
            implementation(libs.kotest.assertions.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

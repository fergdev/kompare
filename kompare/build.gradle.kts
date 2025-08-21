@file:OptIn(ExperimentalComposeLibrary::class, ExperimentalWasmDsl::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)
    id(libs.plugins.androidLint.get().pluginId)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvmToolchain(11)
    applyDefaultHierarchyTemplate()
    androidLibrary {
        namespace = Config.artifactId
        compileSdk = Config.Android.compileSdk
        minSdk = Config.Android.minSdk

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    jvm {}
    js {
        browser()
        nodejs()
        binaries.library()
        binaries.executable() // TODO: https://youtrack.jetbrains.com/projects/KT/issues/KT-80175/K-JS-Task-with-name-jsBrowserProductionWebpack-not-found-in-project
    }
    wasmJs {
        browser()
        nodejs()
        binaries.library()
        binaries.executable() // TODO: https://youtrack.jetbrains.com/projects/KT/issues/KT-80175/K-JS-Task-with-name-jsBrowserProductionWebpack-not-found-in-project
    }

    val xcfName = "${Config.name}Kit"
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = xcfName
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(Config.compilerArgs)
                    optIn.addAll(Config.optIns)
                    progressiveMode.set(true)
                }
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.stdlib)
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.serialization.json)
            implementation(compose.ui)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

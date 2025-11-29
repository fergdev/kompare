@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    applyDefaultHierarchyTemplate()
    android {
        namespace = Config.exampleArtifactId
        compileSdk = Config.Android.compileSdk
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
            }
        }
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {}.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser()
        binaries.executable()
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
            implementation(libs.composeRuntime)
            implementation(libs.composeFoundation)
            implementation(libs.composeUi)
            implementation(libs.material3)
            implementation(libs.composeResources)
            implementation(libs.composePreview)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.composeUiTest)
            implementation(project(":kompare"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.runner)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.ui.test.junit4.android)
            implementation(libs.androidx.espresso.core)
            implementation(project(":kompare"))
        }
        androidUnitTest.dependencies {
            implementation(libs.androidx.testExt.junit)
            implementation(project(":kompare"))
            implementation(libs.androidx.espresso.core)
            implementation(libs.composeUiTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

//compose.desktop {
//    application {
//        mainClass = "${Config.exampleArtifactId}.MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = Config.exampleArtifactId
//            packageVersion = "1.0.1"
//        }
//    }
//}

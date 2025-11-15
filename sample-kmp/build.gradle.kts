@file:OptIn(ExperimentalComposeLibrary::class)
@file:Suppress("UnstableApiUsage")

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
//    alias(libs.plugins.kotlinMultiplatform)
//    alias(libs.plugins.androidApplication)
    id(libs.plugins.androidApplication.get().pluginId)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    applyDefaultHierarchyTemplate()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    androidTarget {
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        compilerOptions {
            jvmTarget.set(Config.jvmTarget)
        }
//        dependencies {
//            androidTestImplementation("androidx.compose.ui:ui-test-junit4-android:1.8.2")
//            debugImplementation("androidx.compose.ui:ui-test-manifest:1.8.2")
//        }
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
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(compose.uiTest)
            implementation(project(":kompare"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.runner)
            implementation(libs.androidx.testExt.junit)
            implementation(libs.ui.test.junit4.android)
            implementation("androidx.test.espresso:espresso-core:3.7.0")
            implementation(project(":kompare"))
        }
        androidUnitTest.dependencies {
            implementation(libs.androidx.testExt.junit)
            implementation(project(":kompare"))
            implementation("androidx.test.espresso:espresso-core:3.7.0")
            implementation(compose.uiTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = Config.exampleArtifactId
    compileSdk = Config.Android.compileSdk

    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        applicationId = Config.exampleArtifactId
        testApplicationId = Config.exampleArtifactId

        minSdk = Config.Android.minSdk
        targetSdk = Config.Android.targetSdk
        versionCode = Config.versionCode

        versionName = Config.majorVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildTypes {
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = Config.javaVersion
        targetCompatibility = Config.javaVersion
    }
}

compose.desktop {
    application {
        mainClass = "${Config.exampleArtifactId}.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = Config.exampleArtifactId
            packageVersion = "1.0.1"
        }
    }
}

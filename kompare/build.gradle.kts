@file:OptIn(ExperimentalComposeLibrary::class, ExperimentalWasmDsl::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)
//    alias(libs.plugins.androidKotlinMultiplatformLibrary)
//    alias(libs.plugins.androidLint)
    id(libs.plugins.androidLint.get().pluginId)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
    id("signing")
}

group = Config.group
version = Config.versionName

kotlin {
    jvmToolchain(11)
    applyDefaultHierarchyTemplate()
    explicitApi()
    withSourcesJar(true)
    androidLibrary {
        namespace = Config.group
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
    }
    wasmJs {
        browser()
        nodejs()
        binaries.library()
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
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.uiTest)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.core.ktx)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(groupId = Config.group, artifactId = "core", version = Config.versionName)

    pom {
        name.set(Config.name)
        description.set(Config.description)
        inceptionYear = "2025"

        url.set(Config.url)
        licenses {
            license {
                name = Config.licenseName
                url = Config.licenseUrl
                distribution = Config.licenseDistribution
            }
        }
        scm {
            url.set(Config.url)
            connection.set("scm:git:git://github.com/fergdev/kompare.git")
            developerConnection.set("scm:git:ssh://git@github.com/fergdev/kompare.git")
        }
        developers {
            developer {
                id.set("fergdev")
                name.set("Fergus Hewson")
            }
        }
    }
}

signing {
    val key = providers.gradleProperty("signingInMemoryKey").orNull
    val pass = providers.gradleProperty("signingInMemoryKeyPassword").orNull
    if (!key.isNullOrBlank()) {
        useInMemoryPgpKeys(key, pass)
    }
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
}

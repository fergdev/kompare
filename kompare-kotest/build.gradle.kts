@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId)
    id(libs.plugins.androidLint.get().pluginId)
    id(libs.plugins.maven.publish.get().pluginId)
    id("signing")
}

kotlin {
    explicitApi()
    applyDefaultHierarchyTemplate()
    androidLibrary {
        namespace = "${Config.group}.kotest"
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
            implementation(libs.kotest)
            implementation(libs.kotest.assertions.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true) // optional but convenient
    signAllPublications()
    coordinates(groupId = Config.group, artifactId = "kotest", version = Config.versionName)

    pom {
        name.set(Config.name)
        description.set(Config.description)
        inceptionYear = Config.inceptionYear

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

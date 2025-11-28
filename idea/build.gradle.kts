import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.intellij.platform") version "2.10.5"
    id(libs.plugins.jetbrains.kotlin.jvm.get().pluginId)
    id(libs.plugins.composeCompiler.get().pluginId)
}

kotlin {
    explicitApi()
    kotlin {
        jvmToolchain(21)

        compilerOptions {
            freeCompilerArgs.addAll(
                listOf(
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
                )
            )
        }
    }
}

group = Config.group
version = Config.versionName

repositories {
    maven("https://packages.jetbrains.team/maven/p/kpm/public/")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    mavenCentral()
    mavenLocal()


    maven { url = uri("https://plugins.gradle.org/m2/") }
    maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}


intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
        }

        changeNotes = """
            Initial version
        """.trimIndent()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )
        bundledModules(
            "intellij.libraries.compose.foundation.desktop",
            "intellij.libraries.skiko",
            "intellij.platform.compose",
            "intellij.platform.jewel.foundation",
            "intellij.platform.jewel.ideLafBridge",
            "intellij.platform.jewel.ui",
        )

        bundledPlugin("org.jetbrains.kotlin")

        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }
    implementation(project(":kompare-image")) {
        isTransitive = false
    }
}

tasks.patchPluginXml {
    sinceBuild.set("241")
//    untilBuild.set("242.*")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
}

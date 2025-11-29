import io.gitlab.arturbosch.detekt.Detekt
import nl.littlerobots.vcu.plugin.versionSelector

plugins {
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.versionCatalogUpdate)
    id(libs.plugins.androidApplication.get().pluginId) apply false
    id(libs.plugins.jetbrains.kotlin.jvm.get().pluginId) apply false
    id(libs.plugins.kotlinMultiplatform.get().pluginId) apply false
    id(libs.plugins.androidKotlinMultiplatformLibrary.get().pluginId) apply false
    id(libs.plugins.androidLint.get().pluginId) apply false
}

dependencies {
    detektPlugins(rootProject.libs.detekt.formatting)
    detektPlugins(rootProject.libs.detekt.compose)
    detektPlugins(rootProject.libs.detekt.libraries)
}

tasks {
    withType<Detekt>().configureEach {
        buildUponDefaultConfig = true
        parallel = true
        setSource(projectDir)
        config.setFrom(File(rootDir, Config.Detekt.configFile))
        basePath = projectDir.absolutePath
        include(Config.Detekt.includedFiles)
        exclude(Config.Detekt.excludedFiles)
        reports {
            xml.required.set(false)
            html.required.set(true)
            txt.required.set(false)
            sarif.required.set(true)
            md.required.set(false)
        }
    }

    register<Detekt>("detektFormat") {
        description = "Formats whole project."
        autoCorrect = true
    }

    register<Detekt>("detektAll") {
        description = "Run detekt on whole project"
        autoCorrect = false
    }
}

versionCatalogUpdate {
    versionSelector {
        !(it.candidate.version.contains("SNAPSHOT", true) ||
                it.candidate.version.contains("ALPHA", true) ||
                it.candidate.version.contains("dev", true))
    }
}
tasks.withType<Sign>().configureEach {
    onlyIf { false }
//    onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
}

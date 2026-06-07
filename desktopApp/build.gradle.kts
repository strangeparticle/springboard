import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Version is defined in gradle.properties
val appVersion = project.findProperty("appVersion")?.toString() ?: error("appVersion not set in gradle.properties")

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.strangeparticle.springboard.app.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Pkg,
                TargetFormat.Dmg,
            )

            packageName = "Springboard"
            packageVersion = appVersion

            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))

                packageName = "Springboard"
                bundleID = "com.strangeparticle.springboard.core"
                appCategory = "public.app-category.developer-tools"
                minimumSystemVersion = "12.0"
            }
        }
    }
}

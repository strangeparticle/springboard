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

        // ProGuard (enabled by default for the release distributable) strips classes that are
        // only reached reflectively via ServiceLoader while leaving their META-INF/services
        // registration files in place. This breaks Ktor's kotlinx-serialization JSON provider
        // (ServiceConfigurationError at launch), so minification is disabled for the packaged build.
        buildTypes.release.proguard {
            isEnabled.set(false)
        }

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

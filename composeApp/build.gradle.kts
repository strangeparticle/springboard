import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.adarshr.test-logger") version "4.0.0"
}

// Version is defined in gradle.properties
val appVersion = project.findProperty("appVersion")?.toString() ?: error("appVersion not set in gradle.properties")

kotlin {
    jvm("desktop")

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
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js.wasm.js)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.uiTestJUnit4)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Always re-run tests (skip Gradle's UP-TO-DATE check) so output is shown every time.
// test-logger plugin settings: use short class names and include skipped tests in output.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    outputs.upToDateWhen { false }

    testlogger {
        showSimpleNames = true
        showSkipped = true
    }
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
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))

                packageName = "Springboard"
                bundleID = "com.strangeparticle.springboard.core"
                appCategory = "public.app-category.developer-tools"
                minimumSystemVersion = "12.0"
            }
        }
    }
}

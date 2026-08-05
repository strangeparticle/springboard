import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.adarshr.test-logger") version "4.0.0"
}

kotlin {
    jvmToolchain(21)

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            // luther-cmp brings luther-core transitively (api). With -PlutherDev the settings.gradle.kts
            // composite build substitutes this coordinate with the local ../luther source.
            implementation("com.strangeparticle:luther-cmp:0.1.1")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.server.auth)
            implementation(libs.ktor.server.status.pages)
            implementation(libs.mcp.kotlin.sdk.server)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.uiTestJUnit4)
            implementation(compose.desktop.currentOs)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js.wasm.js)
        }
    }
}

// Keep the generated Compose resources accessor package stable across the module
// rename (it defaults to <rootProject>.<module>.generated.resources; pinning it
// avoids rewriting every springboard.composeapp.generated.resources import).
compose.resources {
    packageOfResClass = "springboard.composeapp.generated.resources"
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

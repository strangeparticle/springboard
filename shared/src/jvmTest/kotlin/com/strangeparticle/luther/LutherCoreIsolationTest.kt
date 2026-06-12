package com.strangeparticle.luther

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class LutherCoreIsolationTest {
    @Test
    fun lutherPackageHasNoSpringboardImports() {
        val root = lutherSourceRoot()
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file -> file.readLines().any { it.trimStart().startsWith("import com.strangeparticle.springboard") } }
            .map { it.path }
            .toList()
        assertTrue(offenders.isEmpty(), "luther-core must not import springboard:\n${offenders.joinToString("\n")}")
    }

    private fun lutherSourceRoot(): File {
        // The jvmTest working directory differs by setup; try the likely roots and use the first that exists.
        val candidates = listOf(
            File("src/commonMain/kotlin/com/strangeparticle/luther"),
            File("shared/src/commonMain/kotlin/com/strangeparticle/luther"),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate luther source root from working dir ${File(".").absolutePath}")
    }
}

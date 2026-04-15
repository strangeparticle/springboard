package com.strangeparticle.springboard.app.persistence

import com.strangeparticle.springboard.app.settings.persistence.SettingsDto
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PersistenceServiceDesktopImplTest {

    @Test
    fun `load settings returns null when file is absent`() {
        val homeDir = Files.createTempDirectory("springboard-persistence-test-home").toFile()
        val service = PersistenceServiceDesktopImpl(homeDirectoryPath = homeDir.absolutePath)

        assertNull(service.loadSettings())
    }

    @Test
    fun `persist settings writes to springboard_settings json`() {
        val homeDir = Files.createTempDirectory("springboard-persistence-test-home").toFile()
        val service = PersistenceServiceDesktopImpl(homeDirectoryPath = homeDir.absolutePath)

        val dto = SettingsDto(surfaceAppleScriptErrors = true)
        service.persistSettings(dto)

        val expectedFile = File(homeDir, ".springboard/springboard_settings.json")
        assertEquals(true, expectedFile.exists())
        assertEquals(true, service.loadSettings()?.surfaceAppleScriptErrors)
    }

    @Test
    fun `load settings does not fallback to legacy springboard conf json`() {
        val homeDir = Files.createTempDirectory("springboard-persistence-test-home").toFile()
        val legacyDir = File(homeDir, ".springboard").apply { mkdirs() }
        File(legacyDir, "springboard_conf.json").writeText("{\"surfaceAppleScriptErrors\": true}")

        val service = PersistenceServiceDesktopImpl(homeDirectoryPath = homeDir.absolutePath)
        assertNull(service.loadSettings())
    }

    @Test
    fun `persist tabs writes to springboard tabs json`() {
        val homeDir = Files.createTempDirectory("springboard-persistence-test-home").toFile()
        val service = PersistenceServiceDesktopImpl(homeDirectoryPath = homeDir.absolutePath)

        val dto = TabsDto(
            tabs = listOf(TabDto(tabId = "tab-1", source = "https://example.com", zoomPercent = 100)),
            activeTabId = "tab-1",
        )
        service.persistTabs(dto)

        val expectedFile = File(homeDir, ".springboard/springboard_tabs.json")
        assertEquals(true, expectedFile.exists())
        assertEquals("tab-1", service.loadTabs()?.activeTabId)
    }

    @Test
    fun `load settings throws on invalid json`() {
        val homeDir = Files.createTempDirectory("springboard-persistence-test-home").toFile()
        val configDir = File(homeDir, ".springboard").apply { mkdirs() }
        File(configDir, "springboard_settings.json").writeText("not valid json")
        val service = PersistenceServiceDesktopImpl(homeDirectoryPath = homeDir.absolutePath)

        assertFailsWith<IllegalArgumentException> {
            service.loadSettings()
        }
    }

    @Test
    fun `load tabs throws on invalid json`() {
        val homeDir = Files.createTempDirectory("springboard-persistence-test-home").toFile()
        val configDir = File(homeDir, ".springboard").apply { mkdirs() }
        File(configDir, "springboard_tabs.json").writeText("not valid json")
        val service = PersistenceServiceDesktopImpl(homeDirectoryPath = homeDir.absolutePath)

        assertFailsWith<IllegalArgumentException> {
            service.loadTabs()
        }
    }
}
